(() => {
  function postJson(url, csrfToken, body) {
    return fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      },
      body: body ? JSON.stringify(body) : '{}'
    }).then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return response.json();
    });
  }

  function requestSubmit(form) {
    if (typeof form.requestSubmit === 'function') {
      form.requestSubmit();
      return;
    }
    form.submit();
  }

  function loadPayPalSdk(url, userIdToken) {
    return new Promise((resolve, reject) => {
      if (window.paypal) {
        resolve(window.paypal);
        return;
      }

      const existing = document.querySelector('script[data-paypal-sdk-loader="true"]');
      if (existing) {
        existing.addEventListener('load', () => resolve(window.paypal), { once: true });
        existing.addEventListener('error', () => reject(new Error('paypal-sdk-load-failed')), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.src = url;
      script.async = true;
      script.defer = true;
      script.dataset.paypalSdkLoader = 'true';
      if (userIdToken) {
        script.setAttribute('data-user-id-token', userIdToken);
      }
      script.addEventListener('load', () => resolve(window.paypal), { once: true });
      script.addEventListener('error', () => reject(new Error('paypal-sdk-load-failed')), { once: true });
      document.head.appendChild(script);
    });
  }

  function init() {
    const form = document.querySelector('[data-payment-vault-form]');
    if (!form) {
      return;
    }

    const select = form.querySelector('[data-payment-method-select]');
    const providerTokenField = form.querySelector('[data-provider-token-field]');
    const providerTokenInput = form.querySelector('[data-provider-token-input]');
    const providerTokenTypeInput = form.querySelector('[data-provider-token-type-input]');
    const gatewayCustomerReferenceInput = form.querySelector('[data-gateway-customer-reference-input]');
    const displayLabelInput = form.querySelector('[data-display-label-input]');
    const brandInput = form.querySelector('[data-brand-input]');
    const sdkField = form.querySelector('[data-payment-sdk-field]');
    const sdkStatusPanel = form.querySelector('[data-payment-sdk-status-panel]');
    const sdkStatus = form.querySelector('[data-payment-sdk-status]');
    const sdkHelper = form.querySelector('[data-payment-sdk-helper]');
    const sdkMount = form.querySelector('[data-payment-sdk-mount]');
    const saveButton = form.querySelector('[data-manual-save-button]');
    const csrfInput = form.querySelector('input[name="_csrf"]');
    const csrfToken = csrfInput ? csrfInput.value : '';
    const sessionUrlTemplate = form.dataset.paypalSessionUrlTemplate || '';
    const setupUrlTemplate = form.dataset.paypalSetupUrlTemplate || '';

    let renderVersion = 0;

    const setPanelState = (kind, message, helperText) => {
      if (!sdkStatusPanel || !sdkStatus || !sdkHelper) {
        return;
      }
      sdkStatusPanel.classList.remove('is-error', 'is-ready');
      if (kind === 'error') {
        sdkStatusPanel.classList.add('is-error');
      } else if (kind === 'ready') {
        sdkStatusPanel.classList.add('is-ready');
      }
      sdkStatus.textContent = message;
      if (helperText) {
        sdkHelper.textContent = helperText;
      }
    };

    const currentOption = () => select && select.selectedOptions && select.selectedOptions.length ? select.selectedOptions[0] : null;

    const clearSdkMount = () => {
      if (sdkMount) {
        sdkMount.innerHTML = '';
      }
    };

    const switchToManualMode = (helperText) => {
      renderVersion += 1;
      clearSdkMount();
      if (sdkField) {
        sdkField.hidden = true;
      }
      if (providerTokenField) {
        providerTokenField.hidden = false;
      }
      if (saveButton) {
        saveButton.hidden = false;
        saveButton.disabled = false;
      }
      if (providerTokenTypeInput) {
        providerTokenTypeInput.value = '';
      }
      if (helperText && sdkHelper) {
        sdkHelper.textContent = helperText;
      }
    };

    const switchToPayPalMode = async () => {
      const option = currentOption();
      if (!option) {
        switchToManualMode();
        return;
      }
      const methodCode = option.value;
      const providerName = option.dataset.providerBrandName || option.textContent.trim() || 'PayPal';
      const version = ++renderVersion;

      if (sdkField) {
        sdkField.hidden = false;
      }
      if (providerTokenField) {
        providerTokenField.hidden = true;
      }
      if (saveButton) {
        saveButton.hidden = true;
        saveButton.disabled = true;
      }
      setPanelState('ready', 'Inizializzazione PayPal in corso…', 'Il wallet viene collegato nel browser e il backend riceve solo token sicuri.');
      clearSdkMount();

      try {
        const session = await postJson(sessionUrlTemplate.replace('__METHOD__', encodeURIComponent(methodCode)), csrfToken);
        if (version !== renderVersion) {
          return;
        }
        await loadPayPalSdk(session.sdkUrl, session.userIdToken);
        if (version !== renderVersion) {
          return;
        }
        if (!window.paypal || typeof window.paypal.Buttons !== 'function') {
          throw new Error('paypal-sdk-unavailable');
        }

        setPanelState('ready', `${providerName} pronto per collegare un wallet sicuro.`, 'Conferma nel popup PayPal: al termine salveremo solo il token provider, mai i dati grezzi del conto o della carta.');
        window.paypal.Buttons({
          style: {
            layout: 'vertical',
            shape: 'rect',
            label: 'paypal'
          },
          createVaultSetupToken: async () => {
            const setup = await postJson(setupUrlTemplate.replace('__METHOD__', encodeURIComponent(methodCode)), csrfToken);
            form.dataset.lastPaypalSetupToken = setup.setupToken || '';
            if (setup.providerCustomerId && gatewayCustomerReferenceInput && !gatewayCustomerReferenceInput.value) {
              gatewayCustomerReferenceInput.value = setup.providerCustomerId;
            }
            return setup.setupToken;
          },
          onApprove: async (data) => {
            const setupToken = (data && (data.vaultSetupToken || data.setupToken)) || form.dataset.lastPaypalSetupToken;
            if (!setupToken) {
              throw new Error('paypal-setup-token-missing');
            }
            if (providerTokenInput) {
              providerTokenInput.value = setupToken;
            }
            if (providerTokenTypeInput) {
              providerTokenTypeInput.value = 'PAYPAL_SETUP_TOKEN';
            }
            if (displayLabelInput && !displayLabelInput.value) {
              displayLabelInput.value = providerName;
            }
            if (brandInput && !brandInput.value) {
              brandInput.value = 'PayPal';
            }
            setPanelState('ready', 'Token PayPal generato. Salvataggio in corso…');
            requestSubmit(form);
          },
          onCancel: () => {
            setPanelState('error', 'Collegamento PayPal annullato.', 'Puoi riprovare quando vuoi senza inviare dati di pagamento al backend.');
          },
          onError: () => {
            setPanelState('error', 'Impossibile generare il token PayPal.', 'Verifica la configurazione PayPal e riprova.');
          }
        }).render(sdkMount);
      } catch (error) {
        clearSdkMount();
        setPanelState('error', 'Impossibile inizializzare il widget PayPal.', 'Se il problema persiste, controlla la configurazione del metodo di pagamento oppure usa un provider con token già disponibile.');
      }
    };

    const refreshMode = () => {
      const option = currentOption();
      if (!option) {
        switchToManualMode();
        return;
      }
      const mode = (option.dataset.browserTokenizationMode || '').toUpperCase();
      if (mode === 'PAYPAL_JS_SDK') {
        switchToPayPalMode();
        return;
      }
      switchToManualMode();
    };

    if (select) {
      select.addEventListener('change', refreshMode);
      refreshMode();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();

(() => {
  const MOBILE_BREAKPOINT = '(max-width: 960px)';

  function init() {
    // Keep wide operational tables usable without changing their form behaviour.
    document.querySelectorAll('main table.table').forEach((table) => {
      if (!table.closest('.table-wrap')) {
        const wrapper = document.createElement('div');
        wrapper.className = 'table-wrap';
        table.before(wrapper);
        wrapper.append(table);
      }
    });
    document.querySelectorAll('#menu a').forEach((link) => {
      if (new URL(link.href, window.location.href).pathname === window.location.pathname) {
        link.setAttribute('aria-current', 'page');
      }
    });
    const toggles = Array.from(document.querySelectorAll('[data-shell-toggle]'));
    if (!toggles.length || typeof window.matchMedia !== 'function') {
      return;
    }

    const mobileQuery = window.matchMedia(MOBILE_BREAKPOINT);
    document.documentElement.classList.add('shell-ready');
    let lastTouchToggleAt = 0;

    const targetOf = (button) => {
      const id = button.getAttribute('data-shell-toggle');
      return id ? document.getElementById(id) : null;
    };

    const setExpanded = (button, expanded) => {
      const panel = targetOf(button);
      if (!panel) {
        return;
      }
      if (expanded) {
        panel.classList.add('is-open');
        panel.hidden = false;
      } else {
        panel.classList.remove('is-open');
        panel.hidden = mobileQuery.matches;
      }
      button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
    };

    const closeAll = (exceptButton) => {
      toggles.forEach((button) => {
        if (!exceptButton || button !== exceptButton) {
          setExpanded(button, false);
        }
      });
    };

    const syncLayoutState = () => {
      toggles.forEach((button) => {
        const panel = targetOf(button);
        if (!panel) {
          return;
        }
        const isOpen = panel.classList.contains('is-open');
        if (mobileQuery.matches) {
          panel.hidden = !isOpen;
        } else {
          panel.hidden = false;
          panel.classList.remove('is-open');
          button.setAttribute('aria-expanded', 'false');
        }
      });
    };

    const bindToggle = (button) => {
      const panel = targetOf(button);
      if (!panel) {
        return;
      }

      panel.hidden = mobileQuery.matches;
      button.setAttribute('aria-expanded', 'false');

      const toggle = (event) => {
        if (event) {
          event.preventDefault();
          event.stopPropagation();
        }
        if (!mobileQuery.matches) {
          return;
        }
        const willOpen = !panel.classList.contains('is-open');
        closeAll(button);
        setExpanded(button, willOpen);
        if (willOpen) {
          panel.scrollIntoView({ block: 'nearest', inline: 'nearest' });
        }
      };

      button.addEventListener('click', (event) => {
        if (Date.now() - lastTouchToggleAt < 350) {
          event.preventDefault();
          return;
        }
        toggle(event);
      });

      button.addEventListener('touchend', (event) => {
        lastTouchToggleAt = Date.now();
        toggle(event);
      }, { passive: false });
    };

    toggles.forEach(bindToggle);

    const onMediaChange = () => syncLayoutState();
    if (typeof mobileQuery.addEventListener === 'function') {
      mobileQuery.addEventListener('change', onMediaChange);
    } else if (typeof mobileQuery.addListener === 'function') {
      mobileQuery.addListener(onMediaChange);
    }

    window.addEventListener('resize', syncLayoutState);
    window.addEventListener('orientationchange', syncLayoutState);

    document.addEventListener('click', (event) => {
      if (!mobileQuery.matches) {
        return;
      }
      const target = event.target;
      if (!(target instanceof Element)) {
        return;
      }
      if (!target.closest('[data-shell-toggle], #top-links-panel, #menu-links-panel')) {
        closeAll();
      }
    });

    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') {
        const openToggle = toggles.find((button) => button.getAttribute('aria-expanded') === 'true');
        closeAll();
        if (openToggle) openToggle.focus();
      }
    });

    syncLayoutState();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();

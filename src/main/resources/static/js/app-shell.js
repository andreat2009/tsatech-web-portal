(() => {
  const toggles = Array.from(document.querySelectorAll('[data-shell-toggle]'));
  if (!toggles.length) {
    return;
  }

  const mobileQuery = window.matchMedia('(max-width: 960px)');

  const targetOf = (button) => {
    const id = button.getAttribute('data-shell-toggle');
    return id ? document.getElementById(id) : null;
  };

  const closePanel = (button) => {
    const panel = targetOf(button);
    if (!panel) {
      return;
    }
    panel.classList.remove('is-open');
    button.setAttribute('aria-expanded', 'false');
  };

  const closeOthers = (currentButton) => {
    toggles.forEach((button) => {
      if (button !== currentButton) {
        closePanel(button);
      }
    });
  };

  toggles.forEach((button) => {
    const panel = targetOf(button);
    if (!panel) {
      return;
    }
    button.addEventListener('click', () => {
      const willOpen = !panel.classList.contains('is-open');
      closeOthers(button);
      panel.classList.toggle('is-open', willOpen);
      button.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
    });
  });

  const resetDesktopState = () => {
    if (!mobileQuery.matches) {
      toggles.forEach(closePanel);
    }
  };

  mobileQuery.addEventListener('change', resetDesktopState);
  window.addEventListener('resize', resetDesktopState);
  document.addEventListener('click', (event) => {
    if (!mobileQuery.matches) {
      return;
    }
    const clickedInside = event.target.closest('[data-shell-toggle], #top-links-panel, #menu-links-panel');
    if (!clickedInside) {
      toggles.forEach(closePanel);
    }
  });
})();

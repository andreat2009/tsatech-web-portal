(function () {
  const CONSENT_COOKIE = 'tsa_cookie_consent';
  const VISITOR_COOKIE = 'tsa_vid';
  const ONE_YEAR = 60 * 60 * 24 * 365;

  function getCookie(name) {
    const parts = document.cookie.split(';').map(v => v.trim());
    for (const part of parts) {
      if (part.startsWith(name + '=')) {
        return decodeURIComponent(part.substring(name.length + 1));
      }
    }
    return null;
  }

  function setCookie(name, value, maxAgeSeconds) {
    document.cookie = `${name}=${encodeURIComponent(value)}; Path=/; Max-Age=${maxAgeSeconds}; SameSite=Lax`;
  }

  function ensureVisitorId() {
    let visitorId = getCookie(VISITOR_COOKIE);
    if (!visitorId) {
      visitorId = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : ('v-' + Date.now() + '-' + Math.random().toString(16).slice(2));
      setCookie(VISITOR_COOKIE, visitorId, ONE_YEAR);
    }
    return visitorId;
  }

  function parseEntity() {
    const body = document.body;
    const entityType = body?.dataset?.entityType || null;
    const entityId = body?.dataset?.entityId || null;
    return { entityType, entityId };
  }

  function trackPageView() {
    if (getCookie(CONSENT_COOKIE) !== 'accepted') {
      return;
    }
    if (window.location.pathname.startsWith('/admin') || window.location.pathname.startsWith('/amministrazione')) {
      return;
    }

    const { entityType, entityId } = parseEntity();
    fetch('/analytics/track', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        visitorId: ensureVisitorId(),
        eventType: 'page_view',
        path: window.location.pathname + window.location.search,
        pageTitle: document.title,
        referrer: document.referrer || null,
        entityType,
        entityId,
        locale: document.documentElement.lang || null
      })
    }).catch(() => {});
  }

  function bindConsentBanner() {
    const banner = document.getElementById('cookie-consent-banner');
    if (!banner) {
      return;
    }

    const accepted = getCookie(CONSENT_COOKIE);
    if (accepted === 'accepted' || accepted === 'rejected') {
      banner.classList.add('hidden');
      return;
    }

    banner.classList.remove('hidden');

    const acceptBtn = document.getElementById('cookie-accept-btn');
    const rejectBtn = document.getElementById('cookie-reject-btn');

    if (acceptBtn) {
      acceptBtn.addEventListener('click', function () {
        setCookie(CONSENT_COOKIE, 'accepted', ONE_YEAR);
        ensureVisitorId();
        banner.classList.add('hidden');
        trackPageView();
      });
    }

    if (rejectBtn) {
      rejectBtn.addEventListener('click', function () {
        setCookie(CONSENT_COOKIE, 'rejected', ONE_YEAR);
        banner.classList.add('hidden');
      });
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    ensureVisitorId();
    bindConsentBanner();
    trackPageView();
  });
})();

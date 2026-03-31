(function () {
  function submitLanguageForm(select) {
    if (!select) {
      return;
    }
    var form = select.closest('form[data-lang-switcher-form]');
    if (!form) {
      return;
    }
    form.submit();
  }

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-lang-switcher-select]').forEach(function (select) {
      select.addEventListener('change', function () {
        submitLanguageForm(select);
      });
    });
  });
})();

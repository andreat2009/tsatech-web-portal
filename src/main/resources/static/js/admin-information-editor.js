(function () {
  function focusEditor(toolbar) {
    var editorId = toolbar.getAttribute('data-editor-id');
    if (!editorId) {
      return null;
    }
    var editor = document.getElementById(editorId);
    if (!editor) {
      return null;
    }
    editor.focus();
    return editor;
  }

  function applyCommand(toolbar, command, value) {
    var editor = focusEditor(toolbar);
    if (!editor) {
      return;
    }

    if (command === 'createLink') {
      var url = window.prompt('Inserisci URL', 'https://');
      if (!url) {
        return;
      }
      document.execCommand('createLink', false, url);
      return;
    }

    if (command === 'formatBlock' && value) {
      document.execCommand('formatBlock', false, '<' + value + '>');
      return;
    }

    document.execCommand(command, false, value || null);
  }

  function syncEditor(editor) {
    var sourceId = editor.getAttribute('data-source-id');
    if (!sourceId) {
      return;
    }
    var source = document.getElementById(sourceId);
    if (!source) {
      return;
    }
    source.value = editor.innerHTML;
  }

  function initEditor(editor) {
    var sourceId = editor.getAttribute('data-source-id');
    if (!sourceId) {
      return;
    }
    var source = document.getElementById(sourceId);
    if (!source) {
      return;
    }

    editor.innerHTML = source.value || '';

    editor.addEventListener('input', function () {
      syncEditor(editor);
    });

    editor.addEventListener('blur', function () {
      syncEditor(editor);
    });

    syncEditor(editor);
  }

  function initToolbar(toolbar) {
    toolbar.querySelectorAll('button[data-cmd]').forEach(function (btn) {
      btn.addEventListener('click', function (event) {
        event.preventDefault();
        var command = btn.getAttribute('data-cmd');
        applyCommand(toolbar, command, btn.getAttribute('data-value'));
      });
    });

    toolbar.querySelectorAll('select[data-cmd]').forEach(function (select) {
      select.addEventListener('change', function () {
        var command = select.getAttribute('data-cmd');
        var value = select.value;
        applyCommand(toolbar, command, value);
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.wysiwyg-editor').forEach(initEditor);
    document.querySelectorAll('.wysiwyg-toolbar').forEach(initToolbar);

    document.querySelectorAll('form.information-editor-form').forEach(function (form) {
      form.addEventListener('submit', function () {
        form.querySelectorAll('.wysiwyg-editor').forEach(syncEditor);
      });
    });
  });
})();

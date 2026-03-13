(function () {
  const DEFAULT_PAGE_SIZE = 10;

  function parsePageSize(raw) {
    const parsed = Number.parseInt(raw, 10);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
    return DEFAULT_PAGE_SIZE;
  }

  function createButton(label, targetPage, disabled, active, onClick) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'app-pagination-btn';
    if (active) {
      button.classList.add('is-active');
      button.setAttribute('aria-current', 'page');
    }
    button.textContent = label;
    button.disabled = !!disabled;
    if (!disabled && !active) {
      button.addEventListener('click', function () {
        onClick(targetPage);
      });
    }
    return button;
  }

  function buildPageNumbers(totalPages, currentPage) {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, function (_, idx) {
        return idx + 1;
      });
    }

    const pages = [1];
    const start = Math.max(2, currentPage - 1);
    const end = Math.min(totalPages - 1, currentPage + 1);

    if (start > 2) {
      pages.push('...');
    }

    for (let page = start; page <= end; page += 1) {
      pages.push(page);
    }

    if (end < totalPages - 1) {
      pages.push('...');
    }

    pages.push(totalPages);
    return pages;
  }

  function createPager(totalPages, currentPage, onPageChange) {
    if (totalPages <= 1) {
      return null;
    }

    const nav = document.createElement('nav');
    nav.className = 'app-pagination';
    nav.setAttribute('aria-label', 'Pagination');

    nav.appendChild(createButton('‹', currentPage - 1, currentPage <= 1, false, onPageChange));

    const pages = buildPageNumbers(totalPages, currentPage);
    pages.forEach(function (entry) {
      if (entry === '...') {
        const ellipsis = document.createElement('span');
        ellipsis.className = 'app-pagination-ellipsis';
        ellipsis.textContent = '...';
        nav.appendChild(ellipsis);
        return;
      }

      nav.appendChild(createButton(String(entry), entry, false, entry === currentPage, onPageChange));
    });

    nav.appendChild(createButton('›', currentPage + 1, currentPage >= totalPages, false, onPageChange));

    return nav;
  }

  function setupPagination(items, renderItemVisibility, pagerHost, pageSize) {
    const totalItems = items.length;
    const totalPages = Math.ceil(totalItems / pageSize);

    if (totalPages <= 1) {
      return;
    }

    let currentPage = 1;

    function renderPage(page) {
      currentPage = Math.min(Math.max(1, page), totalPages);
      const start = (currentPage - 1) * pageSize;
      const end = start + pageSize;

      items.forEach(function (item, index) {
        renderItemVisibility(item, index >= start && index < end);
      });

      pagerHost.innerHTML = '';
      const pager = createPager(totalPages, currentPage, renderPage);
      if (pager) {
        pagerHost.appendChild(pager);
      }
    }

    renderPage(1);
  }

  function paginateTables() {
    const tables = document.querySelectorAll('table:not([data-paginate="off"])');
    tables.forEach(function (table) {
      const tbody = table.tBodies && table.tBodies.length > 0 ? table.tBodies[0] : null;
      if (!tbody) {
        return;
      }

      const rows = Array.from(tbody.rows).filter(function (row) {
        return !row.hasAttribute('data-pagination-ignore');
      });

      const pageSize = parsePageSize(table.getAttribute('data-paginate'));
      if (rows.length <= pageSize) {
        return;
      }

      const pagerHost = document.createElement('div');
      pagerHost.className = 'app-pagination-host';
      table.insertAdjacentElement('afterend', pagerHost);

      setupPagination(
        rows,
        function (row, visible) {
          row.style.display = visible ? '' : 'none';
        },
        pagerHost,
        pageSize
      );
    });
  }

  function paginateCollections() {
    const collections = document.querySelectorAll('[data-paginate-collection="true"]');
    collections.forEach(function (collection) {
      const selector = collection.getAttribute('data-pagination-item') || ':scope > *';
      const pageSize = parsePageSize(collection.getAttribute('data-paginate'));
      const items = Array.from(collection.querySelectorAll(selector));

      if (items.length <= pageSize) {
        return;
      }

      const pagerHost = document.createElement('div');
      pagerHost.className = 'app-pagination-host';
      collection.insertAdjacentElement('afterend', pagerHost);

      setupPagination(
        items,
        function (item, visible) {
          item.style.display = visible ? '' : 'none';
        },
        pagerHost,
        pageSize
      );
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    paginateTables();
    paginateCollections();
  });
})();

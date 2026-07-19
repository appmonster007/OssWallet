/* PAGE: Records (Transactions) */

async function renderRecords() {
  const params      = new URLSearchParams(location.hash.split('?')[1] || '');
  const accountId   = params.get('accountId') || '';
  const accountName = params.get('accountName') ? decodeURIComponent(params.get('accountName')) : '';
  let page          = parseInt(params.get('page') || '0', 10);
  if (isNaN(page) || page < 0) page = 0;
  // Multi-value filter params (backward-compat: also read old singular names)
  const types          = params.get('types')          || params.get('type')       || '';
  const dateFrom       = params.get('dateFrom') || '';
  const dateTo         = params.get('dateTo')   || '';
  const categoryIds    = params.get('categoryIds')    || params.get('categoryId') || '';
  const statuses       = params.get('statuses')       || params.get('status')     || '';
  const paymentMethods = params.get('paymentMethods') || '';
  const tagNames       = params.get('tagNames')       || '';
  const minAmount      = params.get('minAmount') || '';
  const maxAmount      = params.get('maxAmount') || '';
  const payee          = params.get('payee')     || '';
  const groupBy        = params.get('groupBy') || 'date';
  const sortBy         = params.get('sortBy')  || 'date_desc';
  const pageLimit      = parseInt(localStorage.getItem('oss-page-size') || '25', 10);

  showSpinner();
  try {
    const includePlanned = params.get('includePlanned') !== '0';
    const excludeFuture  = params.get('excludeFuture') === '1';

    let url = `/api/transactions?page=${page}&limit=${pageLimit}`;
    if (accountId)      url += `&accountId=${encodeURIComponent(accountId)}`;
    if (types)          url += `&types=${encodeURIComponent(types)}`;
    if (dateFrom)       url += `&dateFrom=${encodeURIComponent(dateFrom)}`;
    if (dateTo)         url += `&dateTo=${encodeURIComponent(dateTo)}`;
    if (categoryIds)    url += `&categoryIds=${encodeURIComponent(categoryIds)}`;
    if (statuses)       url += `&statuses=${encodeURIComponent(statuses)}`;
    if (paymentMethods) url += `&paymentMethods=${encodeURIComponent(paymentMethods)}`;
    if (tagNames)       url += `&tagNames=${encodeURIComponent(tagNames)}`;
    if (minAmount)      url += `&minAmount=${encodeURIComponent(minAmount)}`;
    if (maxAmount)      url += `&maxAmount=${encodeURIComponent(maxAmount)}`;
    if (payee)          url += `&payee=${encodeURIComponent(payee)}`;
    url += `&includePlanned=${includePlanned}&excludeFuture=${excludeFuture}`;

    const json = await apiFetch(url);
    if (!json.success) { showError('Failed to load records.'); return; }
    const data = json.data || { items: [], total: 0, page: 0, limit: 25, hasMore: false };
    const title = accountName ? `Records — ${esc(accountName)}` : 'Records';

    const today = new Date(); today.setHours(0,0,0,0);
    const fmtD  = d => d.toISOString().slice(0,10);
    const todayStr      = fmtD(today);
    const weekStartStr  = fmtD(new Date(today.getTime() - today.getDay()*86400000));
    const monthStartStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-01`;
    const last30Str     = fmtD(new Date(today.getTime() - 29*86400000));

    const isToday     = dateFrom === todayStr     && dateTo === todayStr;
    const isThisWeek  = dateFrom === weekStartStr  && dateTo === todayStr;
    const isThisMonth = dateFrom === monthStartStr && dateTo === todayStr;
    const isLast30    = dateFrom === last30Str    && dateTo === todayStr;

    // Build quick-filter URL: preserves all non-date params, replaces date range
    const mkQuickUrl = (from, to) => {
      const p = new URLSearchParams();
      if (accountId)      p.set('accountId',      accountId);
      if (accountName)    p.set('accountName',    encodeURIComponent(accountName));
      if (types)          p.set('types',          types);
      if (categoryIds)    p.set('categoryIds',    categoryIds);
      if (statuses)       p.set('statuses',       statuses);
      if (paymentMethods) p.set('paymentMethods', paymentMethods);
      if (tagNames)       p.set('tagNames',       tagNames);
      if (payee)          p.set('payee',          payee);
      if (minAmount)      p.set('minAmount',      minAmount);
      if (maxAmount)      p.set('maxAmount',      maxAmount);
      if (groupBy && groupBy !== 'date')      p.set('groupBy', groupBy);
      if (sortBy  && sortBy  !== 'date_desc') p.set('sortBy',  sortBy);
      if (!includePlanned) p.set('includePlanned', '0');
      if (excludeFuture)   p.set('excludeFuture',  '1');
      if (from) p.set('dateFrom', from);
      if (to)   p.set('dateTo',   to);
      return `#/records?${p.toString()}`;
    };

    // Active filter chips (each chip clears one param, multi-values shown as one chip)
    const _chip = (keys, label) => {
      const p2 = new URLSearchParams(params.toString());
      const keyArr = Array.isArray(keys) ? keys : [keys];
      keyArr.forEach(k => { p2.delete(k); });
      p2.set('page', '0');
      return `<span class="tx-active-chip" onclick="location.hash='#/records?${p2.toString()}'">${esc(label)} <span class="tx-chip-x">×</span></span>`;
    };
    const chips = [];
    if (types) {
      const labels = types.split(',').map(t => prettyEnum(t.trim())).join(', ');
      chips.push(_chip(['types','type'], labels));
    }
    if (statuses) {
      const labels = statuses.split(',').map(s => prettyEnum(s.trim())).join(', ');
      chips.push(_chip(['statuses','status'], labels));
    }
    if (categoryIds) {
      const ids = categoryIds.split(',').map(s => s.trim());
      const names = ids.map(id => cachedCategories.find(c => String(c.id) === id)?.name || `#${id}`);
      chips.push(_chip(['categoryIds','categoryId'], names.length === 1 ? names[0] : `${names.length} categories`));
    }
    if (paymentMethods) {
      const labels = paymentMethods.split(',').map(m => m.trim().replace(/_/g,' ')).join(', ');
      chips.push(_chip('paymentMethods', labels));
    }
    if (tagNames) {
      const tags = tagNames.split(',').map(t => t.trim());
      chips.push(_chip('tagNames', tags.length === 1 ? tags[0] : `${tags.length} tags`));
    }
    if (payee)      chips.push(_chip('payee',      `Payee: ${payee}`));
    if (minAmount)  chips.push(_chip('minAmount',  `≥ ${minAmount}`));
    if (maxAmount)  chips.push(_chip('maxAmount',  `≤ ${maxAmount}`));
    if (dateFrom || dateTo) chips.push(_chip(['dateFrom','dateTo'], dateFrom && dateTo ? `${dateFrom} → ${dateTo}` : (dateFrom || dateTo)));
    const chipsHtml = chips.length
      ? `<div class="tx-active-chips">${chips.join('')}<button class="tx-chip-clear-all" onclick="clearRecordFilters()">Clear all</button></div>`
      : '';

    const activeFilterCount = [types, dateFrom, dateTo, categoryIds, minAmount, maxAmount, statuses, paymentMethods, tagNames, payee].filter(Boolean).length;

    let html = `<div class="page-header">
      <h2 class="section-title">${title}</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddTransaction()">+ Add Record</button>
    </div>
    <div class="tx-filter-pills">
      <button class="tx-pill${isToday    ? ' active' : ''}" onclick="location.hash='${mkQuickUrl(todayStr, todayStr)}'">Today</button>
      <button class="tx-pill${isThisWeek ? ' active' : ''}" onclick="location.hash='${mkQuickUrl(weekStartStr, todayStr)}'">This Week</button>
      <button class="tx-pill${isThisMonth? ' active' : ''}" onclick="location.hash='${mkQuickUrl(monthStartStr, todayStr)}'">This Month</button>
      <button class="tx-pill${isLast30   ? ' active' : ''}" onclick="location.hash='${mkQuickUrl(last30Str, todayStr)}'">Last 30 Days</button>
      <span class="tx-pill-spacer"></span>
      <button class="tx-pill tx-pill-filter${activeFilterCount ? ' active' : ''}" onclick="openRecordFilterSheet()">
        <svg viewBox="0 0 24 24" fill="currentColor" width="13" height="13"><path d="M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z"/></svg>
        Filters${activeFilterCount ? ` (${activeFilterCount})` : ''}
      </button>
    </div>
    ${chipsHtml}`;

    if (data.items.length === 0) {
      html += emptyState('', 'No records found.', 'Clear Filters', 'clearRecordFilters()');
    } else {
      let items = [...data.items];
      if (sortBy === 'date_asc')         items.sort((a,b) => a.date - b.date);
      else if (sortBy === 'amount_desc') items.sort((a,b) => Math.abs(b.amount||0) - Math.abs(a.amount||0));
      else if (sortBy === 'amount_asc')  items.sort((a,b) => Math.abs(a.amount||0) - Math.abs(b.amount||0));
      else if (sortBy === 'title_asc')   items.sort((a,b) => (a.title||'').localeCompare(b.title||''));

      html += `<div class="tx-timeline">`;
      if (groupBy === 'none') {
        html += buildTxTimeline(items, false);
      } else {
        const groups = groupTransactions(items, groupBy);
        groups.forEach(([label, groupItems]) => {
          html += buildTxTimeline(groupItems, true, label);
        });
      }
      html += `</div>`;

      const totalPages = Math.ceil(data.total / data.limit);
      const lastPage   = Math.max(0, totalPages - 1);
      html += `<div class="pagination">
        <span class="page-info">Page ${page+1} of ${totalPages} (${data.total} total)</span>
        <select style="font-size:0.78rem;padding:4px 8px;border:1px solid var(--border);border-radius:var(--radius-sm);background:var(--surface2);color:var(--text)" title="Rows per page" onchange="localStorage.setItem('oss-page-size',this.value);navPage(0)">
          ${[10,25,50,100].map(n=>`<option value="${n}" ${pageLimit===n?'selected':''}>${n} / page</option>`).join('')}
        </select>
        <button class="btn btn-ghost btn-sm" onclick="navPage(0)" ${page===0?'disabled':''} title="First">«</button>
        <button class="btn btn-ghost btn-sm" onclick="navPage(${page-1})" ${page===0?'disabled':''}>← Prev</button>
        <button class="btn btn-ghost btn-sm" onclick="navPage(${page+1})" ${!data.hasMore?'disabled':''}>Next →</button>
        <button class="btn btn-ghost btn-sm" onclick="navPage(${lastPage})" ${!data.hasMore?'disabled':''} title="Last">»</button>
      </div>`;
    }

    setContent(html);

  } catch (e) { if (e.message !== 'Session expired') showError('Error loading records.'); }
}

function buildTxTimeline(items, showGroupHeader, groupLabel) {
  if (!items.length) return '';

  let net = 0;
  items.forEach(tx => {
    if (tx.isPlanned) return;
    const amt = Math.abs(tx.amount || 0);
    if (tx.type === 'INCOME') net += amt;
    else if (tx.type === 'EXPENSE') net -= amt;
  });
  const netCls  = net > 0 ? 'income' : net < 0 ? 'expense' : 'neutral';
  const netSign = net > 0 ? '+' : '';
  const netFmt  = items[0] ? fmt(net, items[0].currency || '') : '';

  let s = '<div class="tx-group">';

  if (showGroupHeader && groupLabel) {
    s += `<div class="tx-day-header">
      <span class="tx-day-label">${esc(groupLabel)}</span>
      <span class="tx-day-net ${netCls}">${netSign}${netFmt}</span>
    </div>`;
  }

  items.forEach(tx => {
    const isIncome   = tx.type === 'INCOME';
    const isExpense  = tx.type === 'EXPENSE';
    const isTransfer = tx.type === 'TRANSFER';
    const sign     = isIncome ? '+' : isExpense ? '−' : '';
    const amtColor = isIncome ? 'var(--income)' : isExpense ? 'var(--expense)' : 'var(--accent)';
    const iconBg   = amtColor;
    const typeIcon = isIncome
      ? `<svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"/></svg>`
      : isTransfer
      ? `<svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"/></svg>`
      : `<svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"/></svg>`;

    const sub  = [tx.accountName||'', tx.categoryName||''].filter(Boolean).join(' · ');
    const note = tx.note ? `<div class="tx-item-note">${esc(tx.note)}</div>` : '';
    const tags = (tx.tags||[]).length
      ? `<div class="tx-item-tags">${tx.tags.map(t=>`<span class="tx-tag">#${esc(t)}</span>`).join('')}</div>` : '';
    const warBadge    = tx.warrantyExpiration ? ` <span class="tx-item-war-badge">W</span>` : '';
    const overduePip  = (tx.isPlanned && tx.isOverdue)  ? `<span class="tx-item-overdue-pill">Overdue</span>` : '';
    const plannedPip  = (tx.isPlanned && !tx.isOverdue) ? `<span class="tx-item-planned-pill">Planned</span>` : '';
    const statusDot   = tx.status && tx.status !== 'CLEARED'
      ? `<span class="tx-item-status-dot" title="${prettyEnum(tx.status)}"></span>` : '';

    let itemCls = 'tx-item';
    if (tx.isPlanned) itemCls += tx.isOverdue ? ' tx-item-overdue' : ' tx-item-planned';

    const clickAction = tx.isPlanned
      ? `openPlannedActionModal(${tx.paymentId}, ${tx.date}, '${esc(tx.title)}')`
      : `openTxDetail(${tx.id})`;

    let actionsHtml;
    if (tx.isPlanned) {
      actionsHtml = `<div class="tx-item-actions">
        <button class="tx-action-btn confirm" onclick="event.stopPropagation();confirmPlannedOccurrence(${tx.paymentId},${tx.date})">✓ Confirm</button>
        <button class="tx-action-btn"         onclick="event.stopPropagation();skipPlannedOccurrence(${tx.paymentId},${tx.date})">Skip</button>
      </div>`;
    } else {
      actionsHtml = `<div class="tx-item-actions">
        <button class="tx-action-btn"        onclick="event.stopPropagation();openEditTransaction(${tx.id})">Edit</button>
        <button class="tx-action-btn"        onclick="event.stopPropagation();cloneTransaction(${tx.id})">Clone</button>
        <button class="tx-action-btn danger" onclick="event.stopPropagation();deleteTransaction('${esc(tx.id)}','${esc(tx.title)}')">Delete</button>
      </div>`;
    }

    s += `<div class="${itemCls}" onclick="${clickAction}">
      <div class="tx-item-icon" style="background:${iconBg}">${typeIcon}</div>
      <div class="tx-item-body">
        <div class="tx-item-row1">
          <span class="tx-item-name">${tx.isPlanned ? '↺ ' : ''}${esc(tx.title)}${warBadge}${overduePip}${plannedPip}</span>
          <span class="tx-item-amount" style="color:${amtColor}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</span>
        </div>
        <div class="tx-item-row2">
          <span class="tx-item-sub">${esc(sub)}</span>
          <span class="tx-item-date">${statusDot}${fmtDate(tx.date)}</span>
        </div>
        ${note}${tags}
      </div>
      ${actionsHtml}
    </div>`;
  });

  return s + '</div>';
}

function buildTxTable(items, noHeader) {
  let s = '';
  if (!noHeader) s += `<table><thead><tr><th>Date</th><th>Title</th><th>Category</th><th>Account</th><th>Type</th><th style="text-align:right">Amount</th><th></th></tr></thead><tbody>`;
  else s += `<table style="border-top:0"><thead style="display:none"><tr><th>Date</th><th>Title</th><th>Category</th><th>Account</th><th>Type</th><th>Amount</th><th></th></tr></thead><tbody>`;
  items.forEach(tx => {
    const amtClass = tx.type === 'INCOME' ? 'amount-income' : tx.type === 'EXPENSE' ? 'amount-expense' : '';
    const sign     = tx.type === 'EXPENSE' ? '−' : tx.type === 'INCOME' ? '+' : '';
    const badgeCls = tx.type === 'INCOME' ? 'badge-income' : tx.type === 'EXPENSE' ? 'badge-expense' : 'badge-transfer';
    const tagsHtml = (tx.tags||[]).length ? `<br>${tx.tags.map(t=>`<span class="badge" style="font-size:0.68rem;background:var(--surface2);color:var(--text)">#${esc(t)}</span>`).join(' ')}` : '';
    const warIcon  = tx.warrantyExpiration ? ' [W]' : '';

    let trCls = '';
    if (tx.isPlanned) {
      trCls = 'tr-planned';
      if (tx.isOverdue) trCls += ' tr-overdue';
    }

    const clickAction = tx.isPlanned ? `openPlannedActionModal(${tx.paymentId}, ${tx.date}, '${esc(tx.title)}')` : `openTxDetail(${tx.id})`;

    s += `<tr class="${trCls}" style="cursor:pointer" onclick="${clickAction}">
      <td style="white-space:nowrap">${fmtDate(tx.date)}</td>
      <td>${tx.isPlanned ? '' : ''}${esc(tx.title)}${warIcon}${tx.note ? `<br><small style="color:var(--text-muted)">${esc(tx.note)}</small>` : ''}${tagsHtml}</td>
      <td>${tx.categoryName ? esc(tx.categoryName) : '<span style="color:var(--text-muted)">—</span>'}</td>
      <td>${esc(tx.accountName || '')}</td>
      <td><span class="badge ${badgeCls}">${prettyEnum(tx.type)}</span></td>
      <td style="text-align:right" class="${amtClass}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</td>
      <td style="white-space:nowrap" onclick="event.stopPropagation()">
        ${tx.isPlanned ? `
          <button class="btn-icon" title="Confirm" onclick="confirmPlannedOccurrence(${tx.paymentId}, ${tx.date})">Confirm</button>
          <button class="btn-icon" title="Skip" onclick="skipPlannedOccurrence(${tx.paymentId}, ${tx.date})">⏭️</button>
        ` : `
          <button class="btn-icon" title="Clone" onclick="cloneTransaction(${tx.id})">Clone</button>
          <button class="btn-icon" title="Edit" onclick="openEditTransaction(${tx.id})">Edit</button>
          <button class="btn-icon" title="Delete" onclick="deleteTransaction('${esc(tx.id)}','${esc(tx.title)}')">Del</button>
        `}
      </td>
    </tr>`;
  });
  return s + '</tbody></table>';
}

window.openPlannedActionModal = function(id, date, title) {
  const body = `
    <div style="text-align:center;padding:10px 0">
      <p style="margin-bottom:20px">Planned occurrence for <strong>${esc(title)}</strong> on ${new Date(date).toLocaleDateString()}.</p>
      <div style="display:flex;gap:12px;justify-content:center">
        <button class="btn btn-primary" onclick="closeModal();confirmPlannedOccurrence(${id}, ${date})">Confirm Now</button>
        <button class="btn btn-ghost" onclick="closeModal();skipPlannedOccurrence(${id}, ${date})">Skip This Time</button>
      </div>
    </div>
  `;
  openModal('Planned Payment', body, null, null);
};

window.confirmPlannedOccurrence = async function(id, date) {
  try {
    const res = await apiWrite('POST', `/api/planned-payments/${id}/confirm-occurrence?date=${date}`);
    if (res.success) {
      showToast('Occurrence confirmed!');
      refreshCurrentView();
    }
    else { showToast('Failed to confirm: ' + (res.error || ''), 'error'); }
  } catch(e) { if (e.message !== 'Session expired') showToast('Error confirming occurrence.', 'error'); }
};

window.skipPlannedOccurrence = async function(id, date) {
  confirmDelete('Skip this occurrence? It will be removed from the list.', async () => {
    try {
      const res = await apiWrite('POST', `/api/planned-payments/${id}/skip-occurrence?date=${date}`);
      closeModal();
      if (res.success) {
        showToast('Occurrence skipped.');
        refreshCurrentView();
      }
      else { showToast('Failed to skip: ' + (res.error || ''), 'error'); }
    } catch(e) { if (e.message !== 'Session expired') showToast('Error skipping occurrence.', 'error'); }
  });
};

function refreshCurrentView() {
  const hash = location.hash || '#/dashboard';
  if      (hash.startsWith('#/dashboard'))        renderDashboard();
  else if (hash.startsWith('#/records'))          renderRecords();
  else if (hash.startsWith('#/analytics'))        renderAnalytics();
  else if (hash.startsWith('#/planned-payments')) renderPlannedPayments();
}

function groupTransactions(items, groupBy) {
  const map = new Map();
  items.forEach(tx => {
    let key;
    if (groupBy === 'date') {
      key = new Date(tx.date).toLocaleDateString(undefined, { weekday:'short', year:'numeric', month:'short', day:'numeric' });
    } else if (groupBy === 'type') {
      key = prettyEnum(tx.type || 'Unknown');
    } else if (groupBy === 'category') {
      key = tx.categoryName || 'Uncategorised';
    } else if (groupBy === 'account') {
      key = tx.accountName || 'Unknown Account';
    } else {
      key = 'All';
    }
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(tx);
  });
  return [...map.entries()];
}

window.openRecordFilterSheet = function() {
  const params         = new URLSearchParams(location.hash.split('?')[1] || '');
  const accountId      = params.get('accountId') || '';
  const curTypes       = (params.get('types') || params.get('type') || '').split(',').filter(Boolean);
  const curCatIds      = (params.get('categoryIds') || params.get('categoryId') || '').split(',').filter(Boolean);
  const curStatuses    = (params.get('statuses') || params.get('status') || '').split(',').filter(Boolean);
  const curMethods     = (params.get('paymentMethods') || '').split(',').filter(Boolean);
  const curTags        = (params.get('tagNames') || '').split(',').filter(Boolean);
  const minAmount      = params.get('minAmount') || '';
  const maxAmount      = params.get('maxAmount') || '';
  const payee          = params.get('payee')     || '';
  const dateFrom       = params.get('dateFrom')  || '';
  const dateTo         = params.get('dateTo')    || '';
  const groupBy        = params.get('groupBy') || 'date';
  const sortBy         = params.get('sortBy')  || 'date_desc';
  const includePlanned = params.get('includePlanned') !== '0';
  const excludeFuture  = params.get('excludeFuture') === '1';

  const mkChips = (name, opts) => opts.map(([val, label]) => `
    <label class="chip-check ${curTypes.includes(val) || curStatuses.includes(val) || curMethods.includes(val) ? 'active' : ''}">
      <input type="checkbox" name="${name}" value="${val}"
        onchange="this.closest('label').classList.toggle('active',this.checked)">
      <span>${label}</span>
    </label>`).join('');

  const typeChips = ['EXPENSE','INCOME','TRANSFER'].map(t => `
    <label class="chip-check ${curTypes.includes(t)?'active':''}">
      <input type="checkbox" name="f-type" value="${t}" ${curTypes.includes(t)?'checked':''}
        onchange="this.closest('label').classList.toggle('active',this.checked)">
      <span>${t.charAt(0)+t.slice(1).toLowerCase()}</span>
    </label>`).join('');

  const statusChips = ['CLEARED','UNCLEARED','RECONCILE'].map(s => `
    <label class="chip-check ${curStatuses.includes(s)?'active':''}">
      <input type="checkbox" name="f-status" value="${s}" ${curStatuses.includes(s)?'checked':''}
        onchange="this.closest('label').classList.toggle('active',this.checked)">
      <span>${s.charAt(0)+s.slice(1).toLowerCase()}</span>
    </label>`).join('');

  const methodChips = ['CASH','CARD','BANK_TRANSFER','DIGITAL_WALLET','OTHER'].map(m => `
    <label class="chip-check ${curMethods.includes(m)?'active':''}">
      <input type="checkbox" name="f-pm" value="${m}" ${curMethods.includes(m)?'checked':''}
        onchange="this.closest('label').classList.toggle('active',this.checked)">
      <span>${m.replace(/_/g,' ')}</span>
    </label>`).join('');

  const catItems = cachedCategories.map(c => ({value: String(c.id), label: c.name}));
  const accItems = cachedAccounts.map(a => ({value: a.id, label: `${a.name} (${a.currency})`}));
  const tagItems = (cachedTags||[]).map(t => ({value: t, label: t}));

  const body = `
    <div class="form-field"><label>Transaction Types</label>
      <div class="chip-group">${typeChips}</div>
    </div>
    <div class="form-field"><label>Status</label>
      <div class="chip-group">${statusChips}</div>
    </div>
    <div class="form-field"><label>Payment Methods</label>
      <div class="chip-group">${methodChips}</div>
    </div>
    ${catItems.length ? `<div class="form-field"><label>Categories</label>${buildMultiSelect('f-cats', catItems, curCatIds, 'Search categories…')}</div>` : ''}
    ${accItems.length ? `<div class="form-field"><label>Accounts</label>${buildMultiSelect('f-accs', accItems, [], 'Search accounts…')}</div>` : ''}
    ${tagItems.length ? `<div class="form-field"><label>Tags</label>${buildMultiSelect('f-tags', tagItems, curTags, 'Search tags…')}</div>` : ''}
    <div class="form-field"><label>Date Range</label>
      <div class="date-range-wrap"><input type="date" id="f-from" value="${esc(dateFrom)}"><span class="date-range-sep">→</span><input type="date" id="f-to" value="${esc(dateTo)}"></div>
    </div>
    <div class="form-row">
      <div class="form-field"><label>Min Amount</label><input type="number" id="f-min" value="${esc(minAmount)}" placeholder="0.00" min="0" step="0.01"></div>
      <div class="form-field"><label>Max Amount</label><input type="number" id="f-max" value="${esc(maxAmount)}" placeholder="Any" min="0" step="0.01"></div>
    </div>
    <div class="form-field"><label>Payee</label><input type="text" id="f-payee" value="${esc(payee)}" placeholder="Search payee…"></div>
    <div class="form-row">
      <div class="form-field"><label>Group By</label>
        ${buildSelect('f-group',[{value:'none',label:'None'},{value:'date',label:'Date'},{value:'type',label:'Type'},{value:'category',label:'Category'},{value:'account',label:'Account'}],groupBy,'None',false)}
      </div>
      <div class="form-field"><label>Sort By</label>
        ${buildSelect('f-sort',[{value:'date_desc',label:'Date ↓'},{value:'date_asc',label:'Date ↑'},{value:'amount_desc',label:'Amount ↓'},{value:'amount_asc',label:'Amount ↑'},{value:'title_asc',label:'Title A→Z'}],sortBy,'Date ↓',false)}
      </div>
    </div>
    <div style="display:flex;gap:20px;padding-top:4px">
      <label style="display:flex;align-items:center;gap:8px;cursor:pointer;font-size:0.88rem">
        <input type="checkbox" id="f-planned" ${includePlanned ? 'checked' : ''} style="width:16px;height:16px"> Include Planned
      </label>
      <label style="display:flex;align-items:center;gap:8px;cursor:pointer;font-size:0.88rem">
        <input type="checkbox" id="f-excl-future" ${excludeFuture ? 'checked' : ''} style="width:16px;height:16px"> Exclude Future
      </label>
    </div>`;

  openFilterSheet('Filter Records', body, applyRecordFilters, clearRecordFilters);
  requestAnimationFrame(() => wireDateRange('f-from', 'f-to'));
};

window.applyRecordFilters = function () {
  const get   = id => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  const cur   = new URLSearchParams(location.hash.split('?')[1] || '');
  const p     = new URLSearchParams();

  // preserve context params
  if (cur.get('accountId'))   p.set('accountId',   cur.get('accountId'));
  if (cur.get('accountName')) p.set('accountName', cur.get('accountName'));

  // chip-group multi-selects
  const selTypes    = [...document.querySelectorAll('input[name="f-type"]:checked')].map(e => e.value);
  const selStatuses = [...document.querySelectorAll('input[name="f-status"]:checked')].map(e => e.value);
  const selMethods  = [...document.querySelectorAll('input[name="f-pm"]:checked')].map(e => e.value);
  // buildMultiSelect components
  const selCats = msGetSelected('f-cats');
  const selAccs = msGetSelected('f-accs');
  const selTags = msGetSelected('f-tags');

  // fall back to current URL values when sheet elements don't exist
  const types    = selTypes.length    ? selTypes.join(',')    : (cur.get('types')          || cur.get('type')       || '');
  const statuses = selStatuses.length ? selStatuses.join(',') : (cur.get('statuses')        || cur.get('status')     || '');
  const methods  = selMethods.length  ? selMethods.join(',')  : (cur.get('paymentMethods')  || '');
  const cats     = selCats.length     ? selCats.join(',')     : (cur.get('categoryIds')     || cur.get('categoryId') || '');
  const accs     = selAccs.length     ? selAccs.join(',')     : (cur.get('accountIds')      || '');
  const tags     = selTags.length     ? selTags.join(',')     : (cur.get('tagNames')        || '');

  if (types)    p.set('types',          types);
  if (statuses) p.set('statuses',       statuses);
  if (methods)  p.set('paymentMethods', methods);
  if (cats)     p.set('categoryIds',    cats);
  if (accs)     p.set('accountIds',     accs);
  if (tags)     p.set('tagNames',       tags);

  const fromVal = get('f-from') || cur.get('dateFrom') || '';
  const toVal   = get('f-to')   || cur.get('dateTo')   || '';
  const minVal  = get('f-min')  || cur.get('minAmount') || '';
  const maxVal  = get('f-max')  || cur.get('maxAmount') || '';
  const payeeV  = get('f-payee') || cur.get('payee')   || '';
  if (fromVal) p.set('dateFrom',   fromVal);
  if (toVal)   p.set('dateTo',     toVal);
  if (minVal)  p.set('minAmount',  minVal);
  if (maxVal)  p.set('maxAmount',  maxVal);
  if (payeeV)  p.set('payee',      payeeV);

  const gBy = get('f-group') || cur.get('groupBy') || 'date';
  const sBy = get('f-sort')  || cur.get('sortBy')  || 'date_desc';
  if (gBy && gBy !== 'date')      p.set('groupBy', gBy);
  if (sBy && sBy !== 'date_desc') p.set('sortBy',  sBy);

  const planned = document.getElementById('f-planned');
  const exclF   = document.getElementById('f-excl-future');
  if (planned && !planned.checked) p.set('includePlanned', '0');
  if (exclF && exclF.checked)      p.set('excludeFuture',  '1');
  p.set('page', '0');
  location.hash = '#/records?' + p.toString();
};

window.clearRecordFilters = function () {
  const cur = new URLSearchParams(location.hash.split('?')[1] || '');
  const p   = new URLSearchParams();
  ['accountId','accountName'].forEach(k => { if (cur.get(k)) p.set(k, cur.get(k)); });
  location.hash = '#/records?' + p.toString();
};

function txFormBody(tx) {
  const cur = tx ? tx.currency : (cachedSettings.baseCurrency || 'USD');
  const warExp = tx?.warrantyExpiration ? new Date(tx.warrantyExpiration).toISOString().slice(0,10) : '';
  const splits = tx?.splits || [];

  return `
    ${formField('Name', `<input type="text" id="tx-name" placeholder="e.g. Grocery run" value="${esc(tx?.title||'')}">`) }
    ${formRow(
      formField('Amount', `<div class="input-group"><span class="input-group-addon" id="tx-amount-prefix">${esc(cur)}</span><input type="number" id="tx-amount" placeholder="0.00" min="0" step="0.01" value="${esc(tx?.amount||'')}" oninput="updateSplitTotal();updateTransferResult()"></div>`),
      formField('Currency', `<input type="text" id="tx-currency" value="${esc(cur)}" maxlength="5" oninput="const p=document.getElementById('tx-amount-prefix');if(p)p.textContent=this.value.toUpperCase()||'—';updateSplitTotal();updateTransferResult()">`)
    )}
    ${formRow(formField('Type', buildSelect('tx-type',[{value:'EXPENSE',label:'Expense'},{value:'INCOME',label:'Income'},{value:'TRANSFER',label:'Transfer'}],(tx?.type||'EXPENSE'),'— Select —',false,"document.getElementById('tx-transfer-fields').style.display=this.value==='TRANSFER'?'':'none';document.getElementById('tx-splits-section').style.display=this.value==='TRANSFER'?'none':'';updateTransferResult();")),
              formField('Account', buildSelect('tx-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),tx?.accountId||'','— Select account —',true)))}

    <div id="tx-transfer-fields" style="${(tx?.type||'EXPENSE')==='TRANSFER'?'':'display:none'}">
      <div class="card" style="padding:12px;background:var(--surface2);border-style:dashed;margin-bottom:14px">
        ${formField('To Account', buildSelect('tx-to-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),tx?.targetAccountId||'','— Select destination —',true,"updateTransferResult()"))}
        <div class="form-row">
          ${formField('Transfer Rate', `<input type="number" id="tx-rate" value="${esc(tx?.transferRate||1)}" step="any" placeholder="1.0" oninput="updateTransferResult()">`)}
          <div class="form-field"><label>Resulting Amount</label><div id="tx-transfer-result" style="font-size:0.9rem;font-weight:700;padding-top:8px">—</div></div>
        </div>
      </div>
    </div>

    <div id="tx-splits-section" style="${(tx?.type||'EXPENSE')==='TRANSFER'?'display:none':''}">
      <div class="form-section-label">Splits</div>
      <div id="tx-splits-container">
        ${splits.map((s, i) => renderSplitRow(i, s)).join('')}
      </div>
      <div id="tx-split-summary" class="split-total-info"></div>
      <button class="btn btn-ghost btn-sm" style="width:100%;margin-bottom:14px" onclick="addSplitRow()">+ Add Split</button>
    </div>

    ${formField('Category', buildSelect('tx-category',[{value:'',label:'— None —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),tx?.categoryId?String(tx.categoryId):'','— None —',true))}
    ${formRow(formField('Date', `<input type="datetime-local" id="tx-date" value="${tx ? new Date(tx.date).toISOString().slice(0,16) : ''}">`),
              formField('Status', buildSelect('tx-status',[{value:'CLEARED',label:'Cleared'},{value:'UNCLEARED',label:'Uncleared'},{value:'RECONCILE',label:'Reconcile'}],(tx?.status||'CLEARED'),'— Select —',false)))}
    ${formRow(formField('Payee', `<input type="text" id="tx-payee" placeholder="Optional payee" value="${esc(tx?.payee||'')}"> `),
              formField('Payment Method', buildSelect('tx-method',[{value:'CASH',label:'Cash'},{value:'CARD',label:'Card'},{value:'BANK_TRANSFER',label:'Bank Transfer'},{value:'DIGITAL_WALLET',label:'Digital Wallet'},{value:'OTHER',label:'Other'}],(tx?.paymentMethod||'CASH'),'— Select —',false)))}

    <div class="form-row">
      ${formField('Location', `<input type="text" id="tx-location" placeholder="e.g. Starbucks, London" value="${esc(tx?.location||'')}">`)}
      ${formField('GPS (Lat,Lon)', `<input type="text" id="tx-gps" placeholder="0.0,0.0" value="${esc(tx?.gpsLocation||'')}">`)}
    </div>

    ${formField('Tags', `<div id="tx-tags-container"></div>`)}
    ${formField('Note', `<textarea id="tx-note" placeholder="Optional note…">${esc(tx?.note||'')}</textarea>`)}
    <div class="form-field" style="flex-direction:row;align-items:center;gap:10px">
      <input type="checkbox" id="tx-warranty" ${tx?.warrantyExpiration?'checked':''}
        onchange="document.getElementById('tx-warranty-wrap').style.display=this.checked?'':'none'">
      <label for="tx-warranty" style="margin-bottom:0;cursor:pointer">Has Warranty</label>
    </div>
    <div id="tx-warranty-wrap" style="${tx?.warrantyExpiration?'':'display:none'}">
      ${formField('Warranty Expiry', `<input type="date" id="tx-warranty-date" value="${esc(warExp)}">`)}
    </div>`;
}

window.renderSplitRow = function(index, split) {
  return `<div class="split-row" id="split-row-${index}">
    <button class="btn-icon btn-icon-danger split-remove" onclick="removeSplitRow(${index})">✕</button>
    <div class="form-row">
      ${formField('Amount', `<input type="number" class="split-amount" data-index="${index}" value="${esc(split?.amount||'')}" placeholder="0.00" step="0.01" oninput="updateSplitTotal()">`)}
      ${formField('Category', buildSelect(`split-cat-${index}`, [{value:'',label:'— None —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))), split?.categoryId?String(split.categoryId):'', '— None —', true))}
    </div>
    <div class="form-field">
      <label>Note</label>
      <input type="text" class="split-note" data-index="${index}" value="${esc(split?.note||'')}" placeholder="Split note…">
    </div>
  </div>`;
};

let splitCounter = 0;
window.addSplitRow = function() {
  const container = document.getElementById('tx-splits-container');
  if (!container) return;
  const div = document.createElement('div');
  const idx = ++splitCounter;
  div.innerHTML = renderSplitRow(idx, null);
  container.appendChild(div.firstElementChild);
  updateSplitTotal();
};

window.removeSplitRow = function(index) {
  const row = document.getElementById(`split-row-${index}`);
  if (row) row.remove();
  updateSplitTotal();
};

window.updateSplitTotal = function() {
  const mainAmt = parseFloat(document.getElementById('tx-amount')?.value) || 0;
  const mainCur = document.getElementById('tx-currency')?.value || '';
  const rows = document.querySelectorAll('.split-row');
  const summary = document.getElementById('tx-split-summary');
  if (!summary) return;

  if (rows.length === 0) {
    summary.style.display = 'none';
    return;
  }
  summary.style.display = 'flex';

  let total = 0;
  rows.forEach(row => {
    const amt = parseFloat(row.querySelector('.split-amount').value) || 0;
    total += amt;
  });

  const diff = mainAmt - total;

  if (Math.abs(diff) < 0.001) {
    summary.className = 'split-total-info split-match';
    summary.innerHTML = `<span>All split amounts match total</span> <span>${fmt(mainAmt, mainCur)}</span>`;
  } else {
    summary.className = 'split-total-info split-mismatch';
    summary.innerHTML = `<span>Remaining to split:</span> <span>${fmt(diff, mainCur)}</span>`;
  }
};

window.updateTransferResult = function() {
  const amt = parseFloat(document.getElementById('tx-amount')?.value) || 0;
  const rate = parseFloat(document.getElementById('tx-rate')?.value) || 1;
  const toAccId = document.getElementById('tx-to-account')?.value;
  const toAcc = cachedAccounts.find(a => a.id === toAccId);
  const resEl = document.getElementById('tx-transfer-result');
  if (resEl) {
    const result = amt * rate;
    resEl.textContent = toAcc ? fmt(result, toAcc.currency) : result.toFixed(2);
  }
};

window.openAddTransaction = async function (prefill) {
  const body = txFormBody(prefill || null);
  openModal('Add Record', body, submitAddTransaction, 'Save Record');
  modalConfBtn.className = 'btn btn-primary';
  initTagChipInput('tx-tags', cachedTags, prefill?.tags || []);
  const dtEl = document.getElementById('tx-date');
  if (dtEl && !prefill) { const now = new Date(); now.setSeconds(0,0); dtEl.value = now.toISOString().slice(0,16); }
  if (prefill?.accountId) { const el = document.getElementById('tx-account'); if (el) el.value = prefill.accountId; }
  if (prefill?.categoryId) { const el = document.getElementById('tx-category'); if (el) el.value = prefill.categoryId; }
};

window.submitAddTransaction = async function () {
  const get = id => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  clearFieldErrors(modalBody);
  const name = get('tx-name');
  if (!name) { showFieldError('tx-name', 'Name is required.'); return; }
  const amount = parseFloat(get('tx-amount'));
  if (isNaN(amount) || amount <= 0) { showFieldError('tx-amount', 'Enter a valid positive amount.'); return; }
  const txType = get('tx-type');
  const warrantyEl = document.getElementById('tx-warranty');
  const warrantyDateStr = warrantyEl?.checked ? get('tx-warranty-date') : '';

  const splits = [];
  if (txType !== 'TRANSFER') {
    document.querySelectorAll('.split-row').forEach(row => {
      const sAmt = parseFloat(row.querySelector('.split-amount').value) || 0;
      if (sAmt > 0) {
        const sIdx = row.id.split('-').pop();
        splits.push({
          amount: sAmt,
          currency: row.querySelector('.split-currency')?.value || get('tx-currency'),
          categoryId: parseInt(document.getElementById(`split-cat-${sIdx}`)?.value) || 0,
          note: row.querySelector('.split-note').value || ''
        });
      }
    });
  }

  const body = {
    name,
    amount,
    currency:         get('tx-currency') || cachedSettings.baseCurrency,
    transactionType:  txType,
    accountId:        get('tx-account') || null,
    categoryId:       parseInt(get('tx-category')) || 0,
    timestamp:        new Date(get('tx-date') || Date.now()).getTime(),
    note:             get('tx-note') || '',
    payee:            get('tx-payee') || '',
    status:           get('tx-status') || 'CLEARED',
    paymentMethod:    get('tx-method') || 'CASH',
    tags:             get('tx-tags') ? get('tx-tags').split(',').map(t=>t.trim()).filter(Boolean) : [],
    warrantyExpiration: warrantyDateStr ? new Date(warrantyDateStr).getTime() : null,
    location:         get('tx-location') || null,
    gpsLocation:      get('tx-gps') || null,
    splits:           splits.length > 0 ? splits : null
  };

  if (txType === 'TRANSFER') {
    const targetAccountId = get('tx-to-account');
    if (!targetAccountId) { showFieldError('tx-to-account', 'Destination account is required for transfers.'); return; }
    if (targetAccountId === body.accountId) { showFieldError('tx-to-account', 'Destination must differ from source account.'); return; }
    body.targetAccountId = targetAccountId;
    body.transferRate = parseFloat(get('tx-rate')) || 1;
  }

  await withLoading(modalConfBtn, async () => {
    const res = await apiWrite('POST', '/api/transactions', body);
    if (res.success) { closeModal(); renderRecords(); showToast('Record saved!'); }
    else { showToast('Failed to save: ' + (res.error || 'Unknown error'), 'error'); }
  });
};

window.openEditTransaction = async function (id) {
  const resp = await apiFetch(`/api/transactions/${id}`);
  if (!resp.success) { showToast('Could not load transaction.', 'error'); return; }
  const tx = resp.data;
  const body = txFormBody(tx);
  openModal('Edit Record', body, async () => {
    const get = i => { const el = document.getElementById(i); return el ? el.value.trim() : ''; };
    clearFieldErrors(modalBody);
    const txType = get('tx-type');
    const warrantyEl = document.getElementById('tx-warranty');
    const warrantyDateStr = warrantyEl?.checked ? get('tx-warranty-date') : '';

    const splits = [];
    if (txType !== 'TRANSFER') {
      document.querySelectorAll('.split-row').forEach(row => {
        const sAmt = parseFloat(row.querySelector('.split-amount').value) || 0;
        if (sAmt > 0) {
          const sIdx = row.id.split('-').pop();
          splits.push({
            amount: sAmt,
            currency: row.querySelector('.split-currency')?.value || get('tx-currency'),
            categoryId: parseInt(document.getElementById(`split-cat-${sIdx}`)?.value) || 0,
            note: row.querySelector('.split-note').value || ''
          });
        }
      });
    }

    const payload = {
      name:             get('tx-name') || undefined,
      amount:           parseFloat(get('tx-amount')) || undefined,
      currency:         get('tx-currency') || undefined,
      transactionType:  txType || undefined,
      accountId:        get('tx-account') || undefined,
      categoryId:       parseInt(get('tx-category')) || undefined,
      timestamp:        new Date(get('tx-date')).getTime() || undefined,
      note:             get('tx-note') || undefined,
      payee:            get('tx-payee') || undefined,
      status:           get('tx-status') || undefined,
      paymentMethod:    get('tx-method') || undefined,
      tags:             get('tx-tags') ? get('tx-tags').split(',').map(t=>t.trim()).filter(Boolean) : [],
      warrantyExpiration: warrantyDateStr ? new Date(warrantyDateStr).getTime() : null,
      location:         get('tx-location') || undefined,
      gpsLocation:      get('tx-gps') || undefined,
      splits:           splits.length > 0 ? splits : null
    };
    if (txType === 'TRANSFER') {
      const targetAccountId = get('tx-to-account');
      if (!targetAccountId) { showFieldError('tx-to-account', 'Destination account is required for transfers.'); return; }
      payload.targetAccountId = targetAccountId;
      payload.transferRate = parseFloat(get('tx-rate')) || 1;
    }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/transactions/${id}`, payload);
      if (res.success) { closeModal(); renderRecords(); showToast('Record updated!'); }
      else { showToast('Failed to update: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
  initTagChipInput('tx-tags', cachedTags, tx.tags || []);
  if (tx.accountId) { const el = document.getElementById('tx-account'); if (el) el.value = tx.accountId; }
  if (tx.categoryId) { const el = document.getElementById('tx-category'); if (el) el.value = tx.categoryId; }
  if (tx.targetAccountId) { const el = document.getElementById('tx-to-account'); if (el) el.value = tx.targetAccountId; }
  updateSplitTotal();
};

window.openTxDetail = async function (id) {
  const resp = await apiFetch(`/api/transactions/${id}`);
  if (!resp.success) { showToast('Could not load transaction.', 'error'); return; }
  const tx = resp.data;

  const isIncome   = tx.type === 'INCOME';
  const isExpense  = tx.type === 'EXPENSE';
  const sign       = isIncome ? '+' : isExpense ? '−' : '';
  const amtColor   = isIncome ? 'var(--income)' : isExpense ? 'var(--expense)' : 'var(--accent)';
  const typeIcon   = isIncome
    ? `<svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"/></svg>`
    : (tx.type === 'TRANSFER')
    ? `<svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"/></svg>`
    : `<svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"/></svg>`;

  const tags = (tx.tags||[]).map(t=>`<span class="pp-tag-chip">#${esc(t)}</span>`).join('');

  const rows = [
    ['Date',     new Date(tx.date).toLocaleString()],
    ['Account',  esc(tx.accountName || '—')],
    tx.targetAccountName ? ['To Account', esc(tx.targetAccountName)] : null,
    ['Category', esc(tx.categoryName || '—')],
    tx.payee           ? ['Payee',    esc(tx.payee)]                           : null,
    tx.status          ? ['Status',   prettyEnum(tx.status)]                   : null,
    tx.paymentMethod   ? ['Method',   prettyEnum(tx.paymentMethod)]            : null,
    tx.location        ? ['Location', esc(tx.location)]                        : null,
    tx.note            ? ['Note',     `<span style="color:var(--text-muted)">${esc(tx.note)}</span>`] : null,
    tags               ? ['Tags',     `<div style="display:flex;gap:4px;flex-wrap:wrap">${tags}</div>`] : null,
    tx.warrantyExpiration ? ['Warranty', new Date(tx.warrantyExpiration).toLocaleDateString()] : null,
    (tx.linkedTransactionIds||[]).length ? ['Linked', tx.linkedTransactionIds.map(lid=>`<a href="#" onclick="event.preventDefault();closeModal();openTxDetail(${lid})" style="color:var(--accent);margin-right:8px">#${lid}</a>`).join('')] : null,
  ].filter(Boolean);

  const body = `<div class="tx-detail">
    <div class="tx-detail-hero">
      <div class="tx-detail-icon" style="background:${amtColor}">${typeIcon}</div>
      <div class="tx-detail-title">${esc(tx.title)}</div>
      <div class="tx-detail-amount" style="color:${amtColor}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</div>
    </div>
    <div class="tx-detail-rows">
      ${rows.map(([k,v]) => `<div class="tx-detail-row"><span class="tx-detail-key">${k}</span><span class="tx-detail-val">${v}</span></div>`).join('')}
    </div>
    <div class="tx-detail-footer">
      <button class="btn btn-ghost btn-sm" onclick="closeModal();openEditTransaction(${tx.id})">Edit</button>
      <button class="btn btn-ghost btn-sm" onclick="closeModal();cloneTransaction(${tx.id})">Clone</button>
      <button class="btn btn-danger btn-sm" onclick="closeModal();deleteTransaction('${tx.id}','${esc(tx.title)}')">Delete</button>
    </div>
  </div>`;
  openModal('Transaction Detail', body, null, null);
};

window.deleteTransaction = function (id, name) {
  confirmDelete(`Delete "${name}"? This action cannot be undone.`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/transactions/${encodeURIComponent(id)}`);
      closeModal();
      if (res.success) { renderRecords(); showToast('Record deleted.'); }
      else { showToast('Failed to delete record.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting record.', 'error'); }
  });
};

window.cloneTransaction = async function (id) {
  try {
    const res = await apiFetch(`/api/transactions/${encodeURIComponent(id)}`);
    if (!res.success) { showToast('Failed to load transaction.', 'error'); return; }
    const tx = res.data;
    openAddTransaction({
      title: tx.title + ' (copy)',
      amount: tx.amount,
      currency: tx.currency,
      type: tx.type,
      accountId: tx.accountId,
      categoryId: tx.categoryId,
      note: tx.note,
      payee: tx.payee,
      tags: tx.tags,
      paymentMethod: tx.paymentMethod,
      location: tx.location,
    });
  } catch (e) { if (e.message !== 'Session expired') showToast('Error cloning transaction.', 'error'); }
};

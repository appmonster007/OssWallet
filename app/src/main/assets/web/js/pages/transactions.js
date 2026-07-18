/* PAGE: Records (Transactions) */

async function renderRecords() {
  const params      = new URLSearchParams(location.hash.split('?')[1] || '');
  const accountId   = params.get('accountId') || '';
  const accountName = params.get('accountName') ? decodeURIComponent(params.get('accountName')) : '';
  let page          = parseInt(params.get('page') || '0', 10);
  if (isNaN(page) || page < 0) page = 0;
  const type        = params.get('type') || '';
  const dateFrom    = params.get('dateFrom') || '';
  const dateTo      = params.get('dateTo') || '';
  const categoryId  = params.get('categoryId') || '';
  const minAmount   = params.get('minAmount') || '';
  const maxAmount   = params.get('maxAmount') || '';
  const status      = params.get('status') || '';
  const payee       = params.get('payee') || '';
  const groupBy     = params.get('groupBy') || 'date';
  const sortBy      = params.get('sortBy') || 'date_desc';
  const filtersOpen = params.get('filtersOpen') === '1';
  const pageLimit   = parseInt(localStorage.getItem('oss-page-size') || '25', 10);

  showSpinner();
  try {
    const includePlanned = params.get('includePlanned') !== '0';
    const excludeFuture  = params.get('excludeFuture') === '1';

    let url = `/api/transactions?page=${page}&limit=${pageLimit}`;
    if (accountId)  url += `&accountId=${encodeURIComponent(accountId)}`;
    if (type)       url += `&type=${encodeURIComponent(type)}`;
    if (dateFrom)   url += `&dateFrom=${encodeURIComponent(dateFrom)}`;
    if (dateTo)     url += `&dateTo=${encodeURIComponent(dateTo)}`;
    if (categoryId) url += `&categoryId=${encodeURIComponent(categoryId)}`;
    if (minAmount)  url += `&minAmount=${encodeURIComponent(minAmount)}`;
    if (maxAmount)  url += `&maxAmount=${encodeURIComponent(maxAmount)}`;
    if (status)     url += `&status=${encodeURIComponent(status)}`;
    if (payee)      url += `&payee=${encodeURIComponent(payee)}`;
    url += `&includePlanned=${includePlanned}&excludeFuture=${excludeFuture}`;

    const json = await apiFetch(url);
    if (!json.success) { showError('Failed to load records.'); return; }
    const data = json.data || { items: [], total: 0, page: 0, limit: 25, hasMore: false };
    const title = accountName ? `Records — ${esc(accountName)}` : 'Records';

    const filterKeys = [type, accountId, dateFrom, dateTo, categoryId, minAmount, maxAmount, status, payee];
    const activeFilterCount = filterKeys.filter(Boolean).length;
    const filterBtnLabel = `⚙️ Filters${activeFilterCount ? ` (${activeFilterCount})` : ''}`;
    const filterBtnCls   = activeFilterCount ? 'btn btn-ghost btn-sm btn-filter-active' : 'btn btn-ghost btn-sm';

    const today = new Date(); today.setHours(0,0,0,0);
    const fmtD  = d => d.toISOString().slice(0,10);
    const todayStr      = fmtD(today);
    const weekStartStr  = fmtD(new Date(today.getTime() - today.getDay()*86400000));
    const monthStartStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-01`;
    const last30Str     = fmtD(new Date(today.getTime() - 29*86400000));

    const fPanel = `<div class="filter-panel${filtersOpen ? '' : ' hidden'}" id="filter-panel">
      <div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:12px">
        <span style="font-size:0.75rem;color:var(--text-muted);align-self:center;padding-right:4px">Quick:</span>
        <button class="btn btn-ghost btn-sm" style="font-size:0.78rem;padding:4px 10px" onclick="setDateQuick('${todayStr}','${todayStr}')">Today</button>
        <button class="btn btn-ghost btn-sm" style="font-size:0.78rem;padding:4px 10px" onclick="setDateQuick('${weekStartStr}','${todayStr}')">This Week</button>
        <button class="btn btn-ghost btn-sm" style="font-size:0.78rem;padding:4px 10px" onclick="setDateQuick('${monthStartStr}','${todayStr}')">This Month</button>
        <button class="btn btn-ghost btn-sm" style="font-size:0.78rem;padding:4px 10px" onclick="setDateQuick('${last30Str}','${todayStr}')">Last 30 Days</button>
      </div>
      <div class="filter-grid">
        <div class="form-field"><label>Type</label>
          ${buildSelect('f-type',[{value:'',label:'All'},{value:'INCOME',label:'Income'},{value:'EXPENSE',label:'Expense'},{value:'TRANSFER',label:'Transfer'}],type||'','All',false)}
        </div>
        <div class="form-field"><label>Account</label>
          ${buildSelect('f-account',[{value:'',label:'All Accounts'}].concat(cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`}))),accountId||'','All Accounts',true)}
        </div>
        <div class="form-field"><label>Category</label>
          ${buildSelect('f-cat',[{value:'',label:'All'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),categoryId||'','All',true)}
        </div>
        <div class="form-field"><label>Status</label>
          ${buildSelect('f-status',[{value:'',label:'All'},{value:'CLEARED',label:'Cleared'},{value:'RECONCILE',label:'Reconcile'},{value:'UNCLEARED',label:'Uncleared'}],status||'','All',false)}
        </div>
        <div class="form-field"><label>Date Range</label><div class="date-range-wrap"><input type="date" id="f-from" value="${esc(dateFrom)}"><span class="date-range-sep">→</span><input type="date" id="f-to" value="${esc(dateTo)}"></div></div>
        <div class="form-field"><label>Min Amount</label><input type="number" id="f-min" value="${esc(minAmount)}" placeholder="0.00" min="0" step="0.01"></div>
        <div class="form-field"><label>Max Amount</label><input type="number" id="f-max" value="${esc(maxAmount)}" placeholder="999999" min="0" step="0.01"></div>
        <div class="form-field"><label>Payee</label><input type="text" id="f-payee" value="${esc(payee)}" placeholder="Search payee…"></div>
        <div class="form-field" style="display:flex;align-items:center;gap:8px;margin-top:24px">
          <input type="checkbox" id="f-planned" ${includePlanned ? 'checked' : ''} style="width:18px;height:18px">
          <label style="margin:0;cursor:pointer" for="f-planned">Include Planned</label>
        </div>
        <div class="form-field" style="display:flex;align-items:center;gap:8px;margin-top:24px">
          <input type="checkbox" id="f-excl-future" ${excludeFuture ? 'checked' : ''} style="width:18px;height:18px">
          <label style="margin:0;cursor:pointer" for="f-excl-future">Exclude Future</label>
        </div>
      </div>
      <div style="display:flex;flex-wrap:wrap;gap:12px;margin-top:12px;padding-top:12px;border-top:1px solid var(--border)">
        <div class="form-field" style="margin:0;flex:1;min-width:160px">
          <label>Group By</label>
          ${buildSelect('f-group',[{value:'none',label:'None'},{value:'date',label:'Date'},{value:'type',label:'Type'},{value:'category',label:'Category'},{value:'account',label:'Account'}],groupBy||'date','None',false)}
        </div>
        <div class="form-field" style="margin:0;flex:1;min-width:200px">
          <label>Sort By</label>
          ${buildSelect('f-sort',[{value:'date_desc',label:'Date (newest first)'},{value:'date_asc',label:'Date (oldest first)'},{value:'amount_desc',label:'Amount (high → low)'},{value:'amount_asc',label:'Amount (low → high)'},{value:'title_asc',label:'Title (A → Z)'}],sortBy||'date_desc','Date (newest first)',false)}
        </div>
      </div>
      <div class="filter-actions" style="margin-top:12px">
        <button class="btn btn-primary btn-sm" onclick="applyRecordFilters()">Apply Filters</button>
        <button class="btn btn-ghost btn-sm" onclick="clearRecordFilters()">Clear All</button>
      </div>
    </div>`;

    let html = `<div class="page-header">
      <h2 class="section-title">${title}</h2>
      <div style="display:flex;gap:8px">
        <button class="${filterBtnCls}" id="filters-btn" onclick="toggleRecordFilters()">${filterBtnLabel}</button>
        <button class="btn btn-primary btn-sm" onclick="openAddTransaction()">+ Add Record</button>
      </div>
    </div>${fPanel}`;

    if (data.items.length === 0) {
      html += emptyState('🔍', 'No records found.', 'Clear Filters', 'clearRecordFilters()');
    } else {
      // Client-side sort (within current page)
      let items = [...data.items];
      if (sortBy === 'date_asc')         items.sort((a,b) => a.date - b.date);
      else if (sortBy === 'amount_desc') items.sort((a,b) => Math.abs(b.amount||0) - Math.abs(a.amount||0));
      else if (sortBy === 'amount_asc')  items.sort((a,b) => Math.abs(a.amount||0) - Math.abs(b.amount||0));
      else if (sortBy === 'title_asc')   items.sort((a,b) => (a.title||'').localeCompare(b.title||''));

      // Timeline layout
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

    if (categoryId) { const el = document.getElementById('f-cat'); if (el) el.value = categoryId; }
    if (accountId)  { const el = document.getElementById('f-account'); if (el) el.value = accountId; }
    if (filtersOpen) { const fp = document.getElementById('filter-panel'); if (fp) fp.classList.remove('hidden'); }
    wireDateRange('f-from', 'f-to');

  } catch (e) { if (e.message !== 'Session expired') showError('Error loading records.'); }
}

function buildTxTimeline(items, showGroupHeader, groupLabel) {
  if (!items.length) return '';

  // Compute group net for display in group header
  let net = 0;
  items.forEach(tx => {
    if (tx.isPlanned) return;
    const amt = Math.abs(tx.amount || 0);
    if (tx.type === 'INCOME') net += amt;
    else if (tx.type === 'EXPENSE') net -= amt;
  });
  const netCls = net > 0 ? 'income' : net < 0 ? 'expense' : 'neutral';
  const netSign = net > 0 ? '+' : '';
  const netFmt = items[0] ? fmt(net, items[0].currency || '') : '';

  let s = '<div class="tx-group">';

  if (showGroupHeader && groupLabel) {
    s += `<div class="tx-group-header">
      <span class="tx-group-label">${esc(groupLabel)}</span>
      <span class="tx-group-total ${netCls}">${netSign}${netFmt}</span>
    </div>`;
  }

  items.forEach(tx => {
    const isIncome   = tx.type === 'INCOME';
    const isExpense  = tx.type === 'EXPENSE';
    const isTransfer = tx.type === 'TRANSFER';
    const sign       = isIncome ? '+' : isExpense ? '−' : '';
    const amtCls     = isIncome ? 'income' : isExpense ? 'expense' : 'transfer';
    const dotCls     = amtCls;
    const tagsHtml   = (tx.tags||[]).length
      ? `<div class="tx-tags">${tx.tags.map(t=>`<span class="tx-tag">#${esc(t)}</span>`).join('')}</div>` : '';
    const warIcon    = tx.warrantyExpiration ? ' 🛡️' : '';

    let rowCls = 'tx-row';
    if (tx.isPlanned) { rowCls += tx.isOverdue ? ' tx-overdue' : ' tx-planned'; }

    const overdueBadge = (tx.isPlanned && tx.isOverdue)
      ? `<span class="badge badge-overdue" style="font-size:0.6rem;margin-left:6px">Overdue</span>` : '';
    const plannedBadge = (tx.isPlanned && !tx.isOverdue)
      ? `<span class="badge badge-active" style="font-size:0.6rem;margin-left:6px">Planned</span>` : '';

    const statusLabel = tx.status && tx.status !== 'CLEARED'
      ? `<div class="tx-status">● ${prettyEnum(tx.status)}</div>` : '';
    const noteHtml = tx.note
      ? `<div class="tx-sub" style="margin-top:1px">${esc(tx.note)}</div>` : '';

    const clickAction = tx.isPlanned
      ? `openPlannedActionModal(${tx.paymentId}, ${tx.date}, '${esc(tx.title)}')`
      : `openTxDetail(${tx.id})`;

    let actionsHtml = '';
    if (tx.isPlanned) {
      actionsHtml = `<div class="tx-actions">
        <button class="btn-icon" style="color:var(--income)" title="Confirm" onclick="event.stopPropagation();confirmPlannedOccurrence(${tx.paymentId},${tx.date})">✓</button>
        <button class="btn-icon" title="Skip" onclick="event.stopPropagation();skipPlannedOccurrence(${tx.paymentId},${tx.date})">⏭</button>
      </div>`;
    } else {
      actionsHtml = `<div class="tx-actions">
        <button class="btn-icon" title="Clone" onclick="event.stopPropagation();cloneTransaction(${tx.id})">⧉</button>
        <button class="btn-icon" title="Edit" onclick="event.stopPropagation();openEditTransaction(${tx.id})">✎</button>
        <button class="btn-icon btn-icon-danger" title="Delete" onclick="event.stopPropagation();deleteTransaction('${esc(tx.id)}','${esc(tx.title)}')">✕</button>
      </div>`;
    }

    s += `<div class="${rowCls}" onclick="${clickAction}">
      <div class="tx-dot ${dotCls}"></div>
      <div class="tx-info">
        <div class="tx-name">${tx.isPlanned ? '↺ ' : ''}${esc(tx.title)}${warIcon}${plannedBadge}${overdueBadge}</div>
        <div class="tx-sub">${esc(tx.accountName||'')}${tx.categoryName ? ' · ' + esc(tx.categoryName) : ''}</div>
        ${noteHtml}
        ${tagsHtml}
      </div>
      <div class="tx-right">
        <div class="tx-amount ${amtCls}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</div>
        ${statusLabel}
        <div class="tx-status">${fmtDate(tx.date)}</div>
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
    const warIcon  = tx.warrantyExpiration ? ' 🛡️' : '';

    let trCls = '';
    if (tx.isPlanned) {
      trCls = 'tr-planned';
      if (tx.isOverdue) trCls += ' tr-overdue';
    }

    const clickAction = tx.isPlanned ? `openPlannedActionModal(${tx.paymentId}, ${tx.date}, '${esc(tx.title)}')` : `openTxDetail(${tx.id})`;

    s += `<tr class="${trCls}" style="cursor:pointer" onclick="${clickAction}">
      <td style="white-space:nowrap">${fmtDate(tx.date)}</td>
      <td>${tx.isPlanned ? '🕒 ' : ''}${esc(tx.title)}${warIcon}${tx.note ? `<br><small style="color:var(--text-muted)">${esc(tx.note)}</small>` : ''}${tagsHtml}</td>
      <td>${tx.categoryName ? esc(tx.categoryName) : '<span style="color:var(--text-muted)">—</span>'}</td>
      <td>${esc(tx.accountName || '')}</td>
      <td><span class="badge ${badgeCls}">${prettyEnum(tx.type)}</span></td>
      <td style="text-align:right" class="${amtClass}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</td>
      <td style="white-space:nowrap" onclick="event.stopPropagation()">
        ${tx.isPlanned ? `
          <button class="btn-icon" title="Confirm" onclick="confirmPlannedOccurrence(${tx.paymentId}, ${tx.date})">✅</button>
          <button class="btn-icon" title="Skip" onclick="skipPlannedOccurrence(${tx.paymentId}, ${tx.date})">⏭️</button>
        ` : `
          <button class="btn-icon" title="Clone" onclick="cloneTransaction(${tx.id})">📋</button>
          <button class="btn-icon" title="Edit" onclick="openEditTransaction(${tx.id})">✏️</button>
          <button class="btn-icon" title="Delete" onclick="deleteTransaction('${esc(tx.id)}','${esc(tx.title)}')">🗑️</button>
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

window.toggleRecordFilters = function () {
  const params = new URLSearchParams(location.hash.split('?')[1] || '');
  const cur = params.get('filtersOpen') === '1';
  params.set('filtersOpen', cur ? '0' : '1');
  location.hash = '#/records?' + params.toString();
};

window.setDateQuick = function (from, to) {
  const fFrom = document.getElementById('f-from');
  const fTo   = document.getElementById('f-to');
  if (fFrom) fFrom.value = from;
  if (fTo)   fTo.value   = to;
};

window.applyRecordFilters = function () {
  const params = new URLSearchParams(location.hash.split('?')[1] || '');
  const get = id => { const el = document.getElementById(id); return el ? el.value : ''; };
  const check = id => { const el = document.getElementById(id); return el ? (el.checked ? '1' : '0') : '0'; };
  params.set('type',       get('f-type'));
  params.set('categoryId', get('f-cat'));
  params.set('accountId',  get('f-account'));
  params.set('dateFrom',   get('f-from'));
  params.set('dateTo',     get('f-to'));
  params.set('minAmount',  get('f-min'));
  params.set('maxAmount',  get('f-max'));
  params.set('status',     get('f-status'));
  params.set('payee',      get('f-payee'));
  params.set('includePlanned', check('f-planned'));
  params.set('excludeFuture',  check('f-excl-future'));
  params.set('groupBy',    get('f-group'));
  params.set('sortBy',     get('f-sort'));
  params.set('page',       '0');
  params.set('filtersOpen','1');
  [...params.keys()].forEach(k => { if (!params.get(k)) params.delete(k); });
  params.set('filtersOpen','1');
  location.hash = '#/records?' + params.toString();
};

window.clearRecordFilters = function () {
  const params = new URLSearchParams(location.hash.split('?')[1] || '');
  const keep = ['accountId','accountName'];
  const fresh = new URLSearchParams();
  keep.forEach(k => { if (params.get(k)) fresh.set(k, params.get(k)); });
  fresh.set('filtersOpen','1');
  location.hash = '#/records?' + fresh.toString();
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
  const amtCls = tx.type==='INCOME'?'amount-income':tx.type==='EXPENSE'?'amount-expense':'';
  const sign   = tx.type==='EXPENSE'?'−':tx.type==='INCOME'?'+':'';
  const badgeCls = tx.type==='INCOME'?'badge-income':tx.type==='EXPENSE'?'badge-expense':'badge-transfer';
  const tags = (tx.tags||[]).map(t=>`<span class="badge" style="background:var(--surface2)">#${esc(t)}</span>`).join(' ');
  const warLine = tx.warrantyExpiration
    ? `<div class="form-field"><label>Warranty Expires</label><div>${new Date(tx.warrantyExpiration).toLocaleDateString()} 🛡️</div></div>`
    : '';
  const locationLine = tx.location
    ? `${formField('Location', `<div>📍 ${esc(tx.location)}</div>`)}`
    : '';
  const linkedLine = (tx.linkedTransactionIds||[]).length
    ? `${formField('Linked Transactions', `<div>${tx.linkedTransactionIds.map(lid=>`<a href="#" onclick="event.preventDefault();closeModal();openTxDetail(${lid})" style="color:var(--accent);margin-right:8px">#${lid}</a>`).join('')}</div>`)}`
    : '';
  const body = `<div style="display:flex;flex-direction:column;gap:10px">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <h3 style="margin:0">${esc(tx.title)}</h3>
      <span class="${amtCls}" style="font-size:1.3rem;font-weight:700">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</span>
    </div>
    <div><span class="badge ${badgeCls}">${prettyEnum(tx.type)}</span> ${tx.status ? `<span class="badge">${prettyEnum(tx.status)}</span>` : ''} ${tx.paymentMethod ? `<span class="badge">${prettyEnum(tx.paymentMethod)}</span>` : ''}</div>
    ${formRow(
      formField('Date', `<div>${new Date(tx.date).toLocaleString()}</div>`),
      formField('Account', `<div>${esc(tx.accountName||'—')}</div>`)
    )}
    ${tx.targetAccountName ? formField('To Account', `<div>${esc(tx.targetAccountName)}</div>`) : ''}
    ${formRow(
      formField('Category', `<div>${esc(tx.categoryName||'—')}</div>`),
      formField('Payee', `<div>${esc(tx.payee||'—')}</div>`)
    )}
    ${tx.note ? formField('Note', `<div style="color:var(--text-muted)">${esc(tx.note)}</div>`) : ''}
    ${tags ? formField('Tags', `<div>${tags}</div>`) : ''}
    ${locationLine}
    ${warLine}
    ${linkedLine}
    <div style="display:flex;gap:8px;margin-top:4px">
      <button class="btn btn-ghost btn-sm" onclick="closeModal();openEditTransaction(${tx.id})">✏️ Edit</button>
      <button class="btn btn-ghost btn-sm" onclick="closeModal();cloneTransaction(${tx.id})">📋 Clone</button>
      <button class="btn btn-danger btn-sm" onclick="closeModal();deleteTransaction('${tx.id}','${esc(tx.title)}')">🗑️ Delete</button>
    </div>
  </div>`;
  openModal(`Transaction Detail`, body, null, null);
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

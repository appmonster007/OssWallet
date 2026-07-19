/* DASHBOARD: Card filter sheet + card management */

const CARD_FILTER_CAPS = {
  NetWorth:              { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true, perspective:true, excludeInternalTransfers:true },
  CashFlow:              { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true, perspective:true, excludeInternalTransfers:true },
  CurrencyBalances:      { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true },
  Accounts:              { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true, perspective:true, excludeInternalTransfers:true },
  Budgets:               { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true },
  PlannedPayments:       { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true },
  Loans:                 { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true },
  RecentActivities:      { timeline:true,  txTypeMode:'none',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true },
  CategoryBreakdown:     { timeline:true,  txTypeMode:'single', accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true, perspective:true, excludeInternalTransfers:true },
  CashFlowComparison:    { timeline:true,  txTypeMode:'both',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:false, refetch:true, perspective:true, excludeInternalTransfers:true, isCumulative:true },
  IncomeExpenseBar:      { timeline:true,  txTypeMode:'both',   accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:false, refetch:true, perspective:true, excludeInternalTransfers:true, isCumulative:true },
  CategorySpendingTrend: { timeline:true,  txTypeMode:'single', accounts:true,  categories:false, currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  nature:true,  refetch:true, perspective:true, excludeInternalTransfers:true, isCumulative:true },
  TagUpSetPlot:          { timeline:true,  txTypeMode:'single', accounts:true,  categories:true,  currencies:true,  tags:true,  statuses:true,  methods:true,  amount:true,  search:true,  future:true,  refetch:true, perspective:true, excludeInternalTransfers:true },
};

window.openCardFilterSheet = function (cardId, payloadType, cardType) {
  const caps = CARD_FILTER_CAPS[payloadType] || {};
  const fs   = cardOverrides[cardId] || {};

  // Fetch full card config to get non-filter settings if possible
  const card = (window.lastDashboardData || window.lastAnalyticsResponse?.cards || []).find(c => c.cardId === cardId);
  const config = card?.config || {};

  let body = `<div style="display:flex;flex-direction:column;gap:14px">`;

  if (caps.perspective) {
    const curP = fs.perspective || config.perspective || 'ACCOUNTING';
    body += `<div class="form-field"><label>Accounting Perspective</label>
      <div class="chip-group">
        <label class="chip-check ${curP==='ACCOUNTING'?'active':''}"><input type="radio" name="cf-perspective" value="ACCOUNTING" ${curP==='ACCOUNTING'?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Accounting</span></label>
        <label class="chip-check ${curP==='CASH_FLOW'?'active':''}"><input type="radio" name="cf-perspective" value="CASH_FLOW" ${curP==='CASH_FLOW'?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Cash Flow</span></label>
      </div></div>`;
  }

  if (caps.timeline) {
    const tl = fs.timelineType || 'ALL_TIME';
    const tlLabels = {ALL_TIME:'All Time',TODAY:'Today',WEEK:'This Week',MONTH:'This Month',QUARTER:'This Quarter',YEAR:'This Year',HALF_YEAR:'Half Year (6mo)',LAST_7_DAYS:'Last 7 Days',LAST_30_DAYS:'Last 30 Days',LAST_6_MONTHS:'Last 6 Months',LAST_1_YEAR:'Last Year',CUSTOM:'Custom Range'};
    body += `<div class="form-field"><label>Period</label>
      ${buildSelect('cf-timeline',Object.keys(tlLabels).map(v=>({value:v,label:tlLabels[v]})),tl,'All Time',false,'toggleCustomDates()')}</div>
    <div id="cf-custom-dates" style="${tl==='CUSTOM'?'':'display:none'}">
      <div class="form-field"><label>Date Range</label><div class="date-range-wrap"><input type="date" id="cf-from" value="${esc(fs.customFrom||'')}"><span class="date-range-sep">→</span><input type="date" id="cf-to" value="${esc(fs.customTo||'')}"></div></div>
    </div>`;
  }

  if (caps.txTypeMode === 'single') {
    const cur = fs.txType || 'EXPENSE';
    body += `<div class="form-field"><label>Transaction Type</label>
      <div class="chip-group">
        <label class="chip-check ${cur==='EXPENSE'?'active':''}"><input type="radio" name="cf-txtype" value="EXPENSE" ${cur==='EXPENSE'?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Expense</span></label>
        <label class="chip-check ${cur==='INCOME'?'active':''}"><input type="radio" name="cf-txtype" value="INCOME"  ${cur==='INCOME'?'checked':''}  onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Income</span></label>
      </div></div>`;
  }

  if (caps.nature) {
    const selNature = fs.natures || config.selectedNatures || [];
    body += `<div class="form-field"><label>Spending Nature</label>` +
      chipCheckboxes('cf-nature', [{value:'MUST',label:'Must'},{value:'NEED',label:'Need'},{value:'WANT',label:'Want'}], selNature) +
      `</div>`;
  }

  if (caps.accounts && cachedAccounts.length) {
    const selAccts = fs.accountIds || [];
    body += `<div class="form-field"><label>Accounts</label>` +
      chipCheckboxes('cf-accounts', cachedAccounts.map(a=>({value:a.id, label:`${a.name} (${a.currency})`})), selAccts) +
      `</div>`;
  }

  if (caps.categories && cachedCategories.length) {
    const selCats = (fs.categoryIds||[]).map(String);
    body += `<div class="form-field"><label>Categories</label>` +
      chipCheckboxes('cf-categories', cachedCategories.map(c=>({value:String(c.id), label:c.name})), selCats) +
      `</div>`;
  }

  if (caps.currencies) {
    const allCurrencies = [...new Set(cachedAccounts.map(a=>a.currency))].sort();
    if (allCurrencies.length) {
      const selCurr = fs.currencies || [];
      body += `<div class="form-field"><label>Currencies</label>` +
        chipCheckboxes('cf-currencies', allCurrencies.map(c=>({value:c,label:c})), selCurr) +
        `</div>`;
    }
  }

  if (caps.statuses) {
    const selSt = fs.statuses || [];
    body += `<div class="form-field"><label>Status</label>` +
      chipCheckboxes('cf-statuses', [{value:'CLEARED',label:'Cleared'},{value:'UNCLEARED',label:'Uncleared'},{value:'RECONCILE',label:'Reconcile'}], selSt) +
      `</div>`;
  }

  if (caps.methods) {
    const selMeth = fs.paymentMethods || [];
    body += `<div class="form-field"><label>Payment Methods</label>` +
      chipCheckboxes('cf-methods', [{value:'CASH',label:'Cash'},{value:'CARD',label:'Card'},{value:'BANK_TRANSFER',label:'Bank Transfer'},{value:'DIGITAL_WALLET',label:'Digital Wallet'},{value:'OTHER',label:'Other'}], selMeth) +
      `</div>`;
  }

  if (caps.tags) {
    const selTags = fs.tags || [];
    if (cachedTags.length) {
      body += `<div class="form-field"><label>Tags</label>` +
        chipCheckboxes('cf-tags-chips', cachedTags.map(t=>({value:t,label:t})), selTags) +
        `</div>`;
    } else {
      body += `<div class="form-field"><label>Tags</label><input type="text" id="cf-tags-text" value="${esc(selTags.join(', '))}" placeholder="Enter tags, comma-separated…"></div>`;
    }
  }

  if (caps.search) {
    body += formRow(
      formField('Search Note', `<input type="text" id="cf-note" value="${esc(fs.queryNote||'')}" placeholder="Filter by note…">`),
      formField('Search Payee', `<input type="text" id="cf-payee" value="${esc(fs.queryPayee||'')}" placeholder="Filter by payee…">`)
    );
  }

  if (caps.amount) {
    body += formRow(
      formField('Min Amount', `<input type="number" id="cf-min" value="${esc(fs.minAmount!=null?fs.minAmount:'')}" placeholder="0.00" min="0" step="0.01">`),
      formField('Max Amount', `<input type="number" id="cf-max" value="${esc(fs.maxAmount!=null?fs.maxAmount:'')}" placeholder="any" min="0" step="0.01">`)
    );
  }

  if (caps.excludeInternalTransfers) {
    const excl = fs.excludeInternalTransfers !== false && config.excludeInternalTransfers !== false;
    body += `<div class="form-field" style="flex-direction:row;align-items:center;gap:10px">
      <input type="checkbox" id="cf-exclude-internal" ${excl?'checked':''}><label for="cf-exclude-internal" style="margin-bottom:0;cursor:pointer">Exclude Internal Transfers</label></div>`;
  }

  if (caps.isCumulative) {
    const cum = fs.isCumulative || config.isCumulative || false;
    body += `<div class="form-field" style="flex-direction:row;align-items:center;gap:10px">
      <input type="checkbox" id="cf-cumulative" ${cum?'checked':''}><label for="cf-cumulative" style="margin-bottom:0;cursor:pointer">Cumulative Chart</label></div>`;
  }

  if (caps.future) {
    const excl = fs.excludeFuture !== false;
    body += `<div class="form-field" style="flex-direction:row;align-items:center;gap:10px">
      <input type="checkbox" id="cf-future" ${excl?'checked':''}><label for="cf-future" style="margin-bottom:0;cursor:pointer" data-tip="Exclude transactions with a future date">Exclude future transactions</label></div>`;
  }

  body += '</div>';

  const hasOverride = Object.keys(fs).length > 0;
  const title = document.querySelector(`[data-card-id="${cardId}"] .dash-card-title`)?.textContent || payloadType;

  openFilterSheet(
    `Filter — ${title}`,
    body,
    () => applyCardFilter(cardId, payloadType, cardType, caps),
    hasOverride ? () => resetCardFilter(cardId, payloadType, cardType) : null
  );
  wireDateRange('cf-from', 'cf-to');
};

window.toggleCustomDates = function () {
  const tl = document.getElementById('cf-timeline')?.value;
  const el = document.getElementById('cf-custom-dates');
  if (el) el.style.display = tl === 'CUSTOM' ? '' : 'none';
};

window.applyCardFilter = async function (cardId, payloadType, cardType, caps) {
  const get = id => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  const getChecked = groupId => getCheckedValues(groupId);
  const getRadio = name => { const el = document.querySelector(`input[name="${name}"]:checked`); return el ? el.value : ''; };

  const fs = {};
  if (caps.perspective) fs.perspective = getRadio('cf-perspective');
  if (caps.timeline) {
    fs.timelineType = get('cf-timeline');
    if (fs.timelineType === 'CUSTOM') { fs.customFrom = get('cf-from'); fs.customTo = get('cf-to'); }
  }
  if (caps.txTypeMode === 'single') {
    const tv = getRadio('cf-txtype');
    if (tv) fs.txType = tv;
  } else if (caps.txTypeMode === 'both') {
    fs.txType = '';
  }
  if (caps.nature)      { const v = getChecked('cf-nature');      if (v.length) fs.natures = v; }
  if (caps.accounts)    { const v = getChecked('cf-accounts');    if (v.length) fs.accountIds = v; }
  if (caps.categories)  { const v = getChecked('cf-categories');  if (v.length) fs.categoryIds = v.map(Number); }
  if (caps.currencies)  { const v = getChecked('cf-currencies');  if (v.length) fs.currencies  = v; }
  if (caps.statuses)    { const v = getChecked('cf-statuses');    if (v.length) fs.statuses    = v; }
  if (caps.methods)     { const v = getChecked('cf-methods');     if (v.length) fs.paymentMethods = v; }
  if (caps.tags) {
    if (cachedTags.length) {
      const v = getChecked('cf-tags-chips'); if (v.length) fs.tags = v;
    } else {
      const raw = get('cf-tags-text'); if (raw) fs.tags = raw.split(',').map(t=>t.trim()).filter(Boolean);
    }
  }
  if (caps.search)      { const n=get('cf-note'); const p=get('cf-payee'); if(n) fs.queryNote=n; if(p) fs.queryPayee=p; }
  if (caps.amount)      { const mn=get('cf-min'); const mx=get('cf-max'); if(mn) fs.minAmount=parseFloat(mn); if(mx) fs.maxAmount=parseFloat(mx); }
  if (caps.excludeInternalTransfers) fs.excludeInternalTransfers = document.getElementById('cf-exclude-internal')?.checked !== false;
  if (caps.isCumulative) fs.isCumulative = document.getElementById('cf-cumulative')?.checked === true;
  if (caps.future)      { fs.excludeFuture = document.getElementById('cf-future')?.checked !== false; }

  const isEmpty = Object.keys(fs).length === 0 || (Object.keys(fs).length === 1 && fs.timelineType === 'ALL_TIME' && !fs.customFrom);

  closeModal();

  if (!isEmpty) {
    const body = {
      perspective:    fs.perspective,
      excludeInternalTransfers: fs.excludeInternalTransfers,
      isCumulative:   fs.isCumulative,
      selectedNatures: fs.natures,
      timelineType: fs.timelineType || 'ALL_TIME',
      dateFrom: fs.customFrom ? new Date(fs.customFrom).getTime() : null,
      dateTo:   fs.customTo   ? new Date(fs.customTo).getTime()   : null,
      accountIds:     fs.accountIds     || [],
      categoryIds:    fs.categoryIds    || [],
      transactionTypes: fs.txType ? [fs.txType] : [],
      tags:           fs.tags           || [],
      currencies:     fs.currencies     || [],
      statuses:       fs.statuses       || [],
      paymentMethods: fs.paymentMethods || [],
      minAmount:      fs.minAmount      ?? null,
      maxAmount:      fs.maxAmount      ?? null,
      queryNote:      fs.queryNote      || null,
      queryPayee:     fs.queryPayee     || null,
      excludeFuture:  fs.excludeFuture  !== false
    };
    await apiWrite('PUT', `/api/dashboard/cards/${cardId}`, body);
    cardOverrides[cardId] = fs;
  } else {
    await apiWrite('PUT', `/api/dashboard/cards/${cardId}`, { timelineType:'ALL_TIME', excludeFuture:true, accountIds:[], categoryIds:[], transactionTypes:[], tags:[], currencies:[], statuses:[], paymentMethods:[] });
    delete cardOverrides[cardId];
  }

  const badgeEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-header .filter-badge`);
  const newBadge = filterBadge(isEmpty ? null : fs);
  if (badgeEl) { badgeEl.outerHTML = newBadge || ''; }
  else if (newBadge) {
    const titleEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-title`);
    if (titleEl) titleEl.insertAdjacentHTML('afterend', newBadge);
  }

  refreshCardContent(cardId, payloadType, cardType);
};

window.resetCardFilter = async function (cardId, payloadType, cardType) {
  delete cardOverrides[cardId];
  closeModal(); closeFilterSheet();
  const badgeEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-header .filter-badge`);
  if (badgeEl) badgeEl.remove();
  await apiWrite('PUT', `/api/dashboard/cards/${cardId}`, { timelineType:'ALL_TIME', excludeFuture:true, accountIds:[], categoryIds:[], transactionTypes:[], tags:[], currencies:[], statuses:[], paymentMethods:[] });
  refreshCardContent(cardId, payloadType, cardType);
};

const ALL_CARD_TYPES = [
  { type:'NET_WORTH',               serialName:'net_worth',                label:'Net Worth',               desc:'Total assets minus liabilities with period cash flow' },
  { type:'CASH_FLOW',               serialName:'cash_flow',                label:'Cash Flow',               desc:'Income vs expenses for the selected period' },
  { type:'CURRENCY_BALANCES',       serialName:'currency_balances',        label:'Currency Balances',       desc:'Account balances grouped by currency with base conversion' },
  { type:'ACCOUNTS',                serialName:'accounts',                 label:'Accounts',                desc:'All account balances, income, and expenses' },
  { type:'BUDGETS',                 serialName:'budgets',                  label:'Budgets',                 desc:'Spending progress against budget limits' },
  { type:'PLANNED_PAYMENTS',        serialName:'planned_payments',         label:'Planned Payments',        desc:'Upcoming and overdue scheduled payments' },
  { type:'LOANS',                   serialName:'loans',                    label:'Loans',                   desc:'Borrowed and lent money with repayment tracking' },
  { type:'RECENT_ACTIVITIES',       serialName:'recent_activities',        label:'Recent Activities',       desc:'Latest transactions at a glance' },
  { type:'CATEGORY_PIE_CHART',      serialName:'category_breakdown',       label:'Category Pie Chart',      desc:'Spending breakdown by category as a pie chart' },
  { type:'CATEGORY_TREEMAP',        serialName:'category_expense_treemap', label:'Category Treemap',        desc:'Visual treemap of category spending proportions' },
  { type:'CASHFLOW_COMPARISON',     serialName:'cashflow_comparison',      label:'Cash Flow Comparison',    desc:'Period-over-period cash flow chart (current vs previous vs last year)' },
  { type:'INCOME_EXPENSE_BAR',      serialName:'income_expense_bar',       label:'Income & Expense Bar',   desc:'Income vs expense bar chart over time' },
  { type:'CATEGORY_SPENDING_TREND', serialName:'category_spending_trend',  label:'Category Spending Trend', desc:'How category spending changes over time' },
  { type:'TAG_UPSET_PLOT',          serialName:'tag_upset_plot',           label:'Tag Analysis',            desc:'Tag co-occurrence and intersection amounts' },
];

window.openManageCards = async function () {
  let cards = [];
  try {
    const res = await apiFetch('/api/dashboard');
    if (res.success) cards = (Array.isArray(res.data) ? res.data : []).sort((a,b)=>(a.order||0)-(b.order||0));
  } catch { /* show empty */ }
  const activeTypes = new Set(cards.map(c => c.cardType));
  const cardIds = cards.map(c => c.cardId);

  let body = '';
  if (cards.length) {
    body += '<div style="font-size:0.78rem;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:.05em;margin-bottom:8px">Active Cards</div>';
    cards.forEach((c, idx) => {
      body += `<div class="manage-card-row">
        <div style="display:flex;gap:3px">
          <button class="btn btn-ghost btn-sm" onclick="reorderDashboardCard(${JSON.stringify(cardIds)},${idx},-1)" ${idx===0?'disabled':''}>↑</button>
          <button class="btn btn-ghost btn-sm" onclick="reorderDashboardCard(${JSON.stringify(cardIds)},${idx},1)" ${idx===cards.length-1?'disabled':''}>↓</button>
        </div>
        <div class="manage-card-name">${esc(c.title||c.cardType)}</div>
        <button class="btn btn-sm" style="background:var(--danger);color:#fff;flex-shrink:0" onclick="removeDashboardCard('${esc(c.cardId)}')">Remove</button>
      </div>`;
    });
  }
  const available = ALL_CARD_TYPES.filter(t => !activeTypes.has(t.type));
  if (available.length) {
    body += `<div style="font-size:0.78rem;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:.05em;margin:${cards.length?'20px':'0'} 0 8px">Add Card</div>
      <div class="add-card-grid">`;
    available.forEach(t => {
      body += `<div class="add-card-btn" onclick="addDashboardCard('${esc(t.serialName)}')">
        <div style="font-weight:600">+ ${esc(t.label)}</div>
        ${t.desc ? `<div style="font-size:0.72rem;color:var(--text-muted);margin-top:2px;line-height:1.3">${esc(t.desc)}</div>` : ''}
      </div>`;
    });
    body += '</div>';
  }
  if (!cards.length && !available.length) body = '<p class="empty-state">No cards available.</p>';

  openModal('Manage Dashboard Cards', body, null, 'Done');
  modalConfBtn.onclick = closeModal;
};

window.addDashboardCard = async function (serialName) {
  try {
    const res = await apiWrite('POST', '/api/dashboard/cards', { cardType: serialName });
    if (res.success) { closeModal(); renderDashboard(); }
    else { showToast('Failed to add card: ' + (res.error || ''), 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error adding card.', 'error'); }
};

window.removeDashboardCard = function (cardId) {
  confirmDelete('Remove this card from your dashboard?', async () => {
    try {
      const res = await apiWrite('DELETE', `/api/dashboard/cards/${encodeURIComponent(cardId)}`);
      if (res.success) { closeModal(); renderDashboard(); showToast('Card removed.'); }
      else { showToast('Failed to remove card: ' + (res.error || ''), 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error removing card.', 'error'); }
  });
};

window.reorderDashboardCard = async function (cardIds, idx, dir) {
  const newIds = [...cardIds];
  const swap = idx + dir;
  if (swap < 0 || swap >= newIds.length) return;
  [newIds[idx], newIds[swap]] = [newIds[swap], newIds[idx]];
  try {
    const res = await apiWrite('PUT', '/api/dashboard/cards/reorder', { cardIds: newIds });
    if (res.success) { closeModal(); renderDashboard(); }
    else { showToast('Failed to reorder: ' + (res.error || ''), 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error reordering.', 'error'); }
};

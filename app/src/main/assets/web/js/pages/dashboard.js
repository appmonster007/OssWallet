/* PAGE: Dashboard + all card body renderers */

async function renderDashboard() {
  showSpinner();
  try {
    const json = await apiFetch('/api/dashboard');
    if (!json.success) { showError('Failed to load dashboard.'); return; }
    const cards = (Array.isArray(json.data) ? json.data : [])
      .filter(c => c.isEnabled !== false)
      .sort((a, b) => (a.order || 0) - (b.order || 0));

    cards.forEach(card => {
      const fs = card.filterSummary;
      if (fs && !cardOverrides[card.cardId]) {
        cardOverrides[card.cardId] = {
          timelineType:   fs.timelineType   || 'ALL_TIME',
          customFrom: fs.dateFrom ? new Date(fs.dateFrom).toISOString().slice(0,10) : null,
          customTo:   fs.dateTo   ? new Date(fs.dateTo).toISOString().slice(0,10)   : null,
          accountIds:     fs.accountIds      || [],
          categoryIds:    fs.categoryIds     || [],
          txType:         (fs.transactionTypes||[])[0] || '',
          currencies:     fs.currencies      || [],
          statuses:       fs.statuses        || [],
          paymentMethods: fs.paymentMethods  || [],
          tags:           fs.tags            || [],
          queryNote:      fs.queryNote       || '',
          queryPayee:     fs.queryPayee      || '',
          minAmount:      fs.minAmount       ?? null,
          maxAmount:      fs.maxAmount       ?? null,
          excludeFuture:  fs.excludeFuture   !== false,
        };
      }
    });

    const pinnedCard = cards.find(c => c.cardType === 'ACCOUNTS');
    const regularCards = pinnedCard ? cards.filter(c => c.cardId !== pinnedCard.cardId) : cards;

    let html = `<div class="page-header">
      <h2 class="section-title">Dashboard</h2>
      <button class="btn btn-ghost btn-sm" onclick="openManageCards()">⚙️ Manage Cards</button>
    </div>`;

    if (pinnedCard) {
      const d = pinnedCard.data || {};
      const payloadType = d.type ? d.type.split('.').pop() : 'Accounts';
      html += pinnedAccountsWrap(pinnedCard, dispatchCardBody(pinnedCard, d, payloadType), payloadType);
    }

    if (!regularCards.length && !pinnedCard) {
      html += '<div class="alert alert-warn">No dashboard cards are enabled. Click <strong>Manage Cards</strong> to add some.</div>';
    } else {
      regularCards.forEach(card => {
        const d = card.data || {};
        const payloadType = d.type ? d.type.split('.').pop() : '';
        html += dispatchCardRender(card, d, payloadType);
      });
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading dashboard.'); }
}

async function refreshCardContent(cardId, payloadType, cardType) {
  const el = document.getElementById('card-body-' + cardId);
  if (!el) return;
  el.innerHTML = '<div class="loading-wrap"><div class="spinner" style="width:20px;height:20px;border-width:2px;margin:20px auto"></div></div>';
  try {
    const resp = await apiFetch('/api/dashboard');
    const cards = resp.success ? (resp.data || []) : [];
    const card = cards.find(c => c.cardId === cardId);
    if (!card) { el.innerHTML = '<div class="alert alert-warn" style="font-size:0.85rem">Card not found in dashboard.</div>'; return; }
    const d  = card.data;
    const pt = d.type ? d.type.split('.').pop() : payloadType;
    el.innerHTML = dispatchCardBody(card, d, pt);
    const badgeEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-header .filter-badge`);
    const newBadge = filterBadge(card.filterSummary);
    if (badgeEl) { badgeEl.outerHTML = newBadge || ''; }
    else if (newBadge) { const titleEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-title`); if (titleEl) titleEl.insertAdjacentHTML('afterend', newBadge); }
  } catch (e) {
    if (e.message !== 'Session expired') el.innerHTML = '<div class="alert alert-error">Error refreshing card.</div>';
  }
}

/* ── Card body renderers ── */

function renderNetWorthBody(d) {
  const cur = d.currency || cachedSettings.baseCurrency || 'USD';
  const nwCls = (d.netWorth||0) >= 0 ? 'positive' : 'negative';
  const cfCls = (d.cashFlow||0) >= 0 ? 'positive' : 'negative';
  const hasTransfer = d.transfer != null && d.transfer !== 0;
  let html = `<div class="card-grid" style="margin-bottom:0">
    <div class="stat-card"><div class="stat-label">Net Worth</div><div class="stat-value ${nwCls}">${fmt(d.netWorth||0,cur)}</div></div>
    <div class="stat-card"><div class="stat-label">Income</div><div class="stat-value positive">+${fmt(d.income||0,cur)}</div></div>
    <div class="stat-card"><div class="stat-label">Expenses</div><div class="stat-value negative">−${fmt(d.expense||0,cur)}</div></div>`;
  if (hasTransfer) html += `<div class="stat-card"><div class="stat-label">Transfers</div><div class="stat-value">${fmt(d.transfer||0,cur)}</div></div>`;
  html += `<div class="stat-card"><div class="stat-label">Cash Flow</div><div class="stat-value ${cfCls}">${(d.cashFlow||0)>=0?'+':'−'}${fmt(Math.abs(d.cashFlow||0),cur)}</div></div>`;
  if (d.previousNetWorth != null && d.previousNetWorth !== 0) {
    const delta = (d.netWorth||0) - d.previousNetWorth;
    const pct = (delta / Math.abs(d.previousNetWorth) * 100).toFixed(1);
    const pctCls = delta >= 0 ? 'positive' : 'negative';
    html += `<div class="stat-card"><div class="stat-label">vs Previous</div><div class="stat-value ${pctCls}">${delta>=0?'+':'−'}${Math.abs(pct)}%</div></div>`;
  }
  return html + '</div>';
}

function renderCashFlowBody(d) {
  const cur = d.currency || cachedSettings.baseCurrency || 'USD';
  const netCls = (d.net||0) >= 0 ? 'positive' : 'negative';
  const hasTransfer = d.transfer != null && d.transfer !== 0;
  let html = `<div class="card-grid" style="margin-bottom:0">
    <div class="stat-card"><div class="stat-label">Income</div><div class="stat-value positive">+${fmt(d.income||0,cur)}</div></div>
    <div class="stat-card"><div class="stat-label">Expense</div><div class="stat-value negative">−${fmt(d.expense||0,cur)}</div></div>`;
  if (hasTransfer) html += `<div class="stat-card"><div class="stat-label">Transfers</div><div class="stat-value">${fmt(d.transfer||0,cur)}</div></div>`;
  html += `<div class="stat-card"><div class="stat-label">Net</div><div class="stat-value ${netCls}">${(d.net||0)>=0?'+':'−'}${fmt(Math.abs(d.net||0),cur)}</div></div>`;
  return html + '</div>';
}

function renderCurrencyBalancesBody(d) {
  const items = d.items || [];
  const baseCur = d.baseCurrency || cachedSettings.baseCurrency || '';
  if (!items.length) return '<p class="empty-state">No currency balances.</p>';
  let body = '<div class="currency-grid">';
  items.forEach(item => {
    const cls = (item.balance||0) >= 0 ? 'amount-income' : 'amount-expense';
    body += `<div class="currency-item">
      <div class="currency-code">${esc(item.currency)}</div>
      <div class="currency-balance ${cls}">${fmt(item.balance||0, item.currency)}</div>
      ${baseCur && item.currency !== baseCur && item.balanceInBase != null
        ? `<div class="currency-base">≈ ${fmt(item.balanceInBase, baseCur)}</div>` : ''}
    </div>`;
  });
  return body + '</div>';
}

function renderAccountsBody(d, baseCur) {
  const accounts = d.accounts || [];
  const bCur = baseCur || cachedSettings.baseCurrency || '';
  if (!accounts.length) return '<p class="empty-state">No accounts.</p>';
  let body = '<div class="card-grid" style="margin-bottom:0">';
  accounts.forEach(acc => {
    const cls = (acc.balance||0) >= 0 ? 'positive' : 'negative';
    const hasTransfer = acc.transfer != null && acc.transfer !== 0;
    const showBase = bCur && acc.currency !== bCur && acc.balanceInBase != null;
    body += `<div class="stat-card" style="cursor:pointer" onclick="navigate('#/accounts/${esc(acc.id)}')">
      <div style="font-weight:600;margin-bottom:6px">${esc(acc.name)}</div>
      <div class="stat-value ${cls}" style="font-size:1.1rem">${fmt(acc.balance||0,acc.currency)}</div>
      ${showBase ? `<div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">≈ ${fmt(acc.balanceInBase,bCur)}</div>` : ''}
      ${acc.income != null ? `<div style="font-size:0.78rem;margin-top:6px;display:flex;gap:10px;flex-wrap:wrap">
        <span class="amount-income">+${fmt(acc.income||0,acc.currency)}</span>
        <span class="amount-expense">−${fmt(acc.expense||0,acc.currency)}</span>
        ${hasTransfer?`<span style="color:var(--text-muted)">↔${fmt(Math.abs(acc.transfer||0),acc.currency)}</span>`:''}
      </div>` : ''}
      <div style="margin-top:8px;display:flex;gap:6px">
        <a class="btn btn-primary btn-sm" href="#/accounts/${esc(acc.id)}" onclick="event.stopPropagation()">Details</a>
        <a class="btn btn-ghost btn-sm" href="#/records?accountId=${esc(acc.id)}&accountName=${encodeURIComponent(acc.name)}" onclick="event.stopPropagation()">Records</a>
      </div>
    </div>`;
  });
  return body + '</div>';
}

function renderBudgetBody(d) {
  const items = d.items || [];
  if (!items.length) return '<p class="empty-state">No budgets.</p>';
  let body = '';
  items.forEach(b => {
    const pct = Math.min(100, Math.round(b.percentage||0));
    const alertCls = b.alertLevel==='EXCEEDED'?'progress-red':b.alertLevel==='WARNING'?'progress-orange':'progress-green';
    const archived = b.isArchived ? '<span class="badge" style="background:var(--border);color:var(--text-muted);font-size:0.68rem">Archived</span>' : '';
    const statusBadge = b.alertLevel==='EXCEEDED'
      ? '<span class="badge" style="background:var(--danger);color:#fff;font-size:0.65rem;margin-left:4px">OVER BUDGET</span>'
      : b.alertLevel==='WARNING'
      ? '<span class="badge" style="background:var(--warning);color:#fff;font-size:0.65rem;margin-left:4px">WARNING</span>'
      : '';
    body += `<div style="margin-bottom:14px;cursor:pointer" onclick="navigate('#/budgets/${esc(b.id)}')">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;gap:8px">
        <span style="font-weight:600;flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${esc(b.name)} ${archived}${statusBadge}</span>
        <span style="font-size:0.78rem;color:var(--text-muted);white-space:nowrap">${esc(b.period||'')}</span>
        <span style="font-weight:700;white-space:nowrap;color:${b.alertLevel==='EXCEEDED'?'var(--danger)':b.alertLevel==='WARNING'?'var(--warning)':'var(--success)'}">${pct}%</span>
      </div>
      ${progressBar(pct,alertCls)}
      <div style="display:flex;justify-content:space-between;font-size:0.78rem;color:var(--text-muted);margin-top:3px;flex-wrap:wrap;gap:4px">
        <span>Spent: ${fmt(b.spent||0,b.currency)}</span>
        <span>Remaining: ${fmt(b.remaining||0,b.currency)}</span>
        <span>Limit: ${fmt(b.limitAmount||0,b.currency)}</span>
      </div>
    </div>`;
  });
  return body;
}

function renderPlannedPaymentsBody(d) {
  const upcoming = d.upcoming||[], overdue = d.overdue||[];
  if (!upcoming.length && !overdue.length) return '<p class="empty-state">No upcoming or overdue payments.</p>';
  let body = '';
  if (overdue.length) {
    body += `<div style="margin-bottom:12px"><div class="overdue-header" style="font-size:0.9rem">⚠️ Overdue (${overdue.length})</div>
      <div class="table-wrap"><table><thead><tr><th>Name</th><th>Amount</th><th>Due</th><th>Account</th><th>Actions</th></tr></thead><tbody>`;
    overdue.forEach(p => {
      body += `<tr><td>${esc(p.name)}<br><small style="color:var(--text-muted)">${esc(p.recurrenceDescription||'')}</small></td><td>${fmt(p.amount||0,p.currency)}</td><td style="color:var(--danger);white-space:nowrap">${fmtDate(p.nextDueDate)}</td><td>${esc(p.accountName||'—')}</td><td style="white-space:nowrap"><button class="btn btn-sm btn-primary" onclick="confirmPlanned('${esc(p.id)}')">✓</button> <button class="btn btn-sm btn-ghost" onclick="skipPlanned('${esc(p.id)}')">Skip</button></td></tr>`;
    });
    body += '</tbody></table></div></div>';
  }
  if (upcoming.length) {
    body += `<div><div class="upcoming-header" style="font-size:0.9rem">📅 Upcoming (${upcoming.length})</div>
      <div class="table-wrap"><table><thead><tr><th>Name</th><th>Amount</th><th>Due</th><th>Recurrence</th><th>Category</th></tr></thead><tbody>`;
    upcoming.forEach(p => {
      body += `<tr><td>${esc(p.name)}</td><td>${fmt(p.amount||0,p.currency)}</td><td style="white-space:nowrap">${fmtDate(p.nextDueDate)}</td><td><small style="color:var(--text-muted)">${esc(p.recurrenceDescription||'—')}</small></td><td>${esc(p.categoryName||'—')}</td></tr>`;
    });
    body += '</tbody></table></div></div>';
  }
  return body;
}

function renderLoansBody(d) {
  const items = d.items||[];
  if (!items.length) return '<p class="empty-state">No loans.</p>';
  let body = '<div class="card-grid" style="margin-bottom:0">';
  items.forEach(loan => {
    const rp = Math.min(100, Math.round(((loan.totalPaid||0)/(loan.principalAmount||1))*100));
    const tb = loan.loanType==='LENT'?'badge-lent':'badge-borrowed';
    const sb = loan.status==='SETTLED'?'badge-settled':'badge-active';
    body += `<div class="stat-card" style="cursor:pointer" onclick="navigate('#/loans/${esc(loan.id)}')">${
      `<div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:8px"><div><div style="font-weight:700">${esc(loan.name)}</div><div style="font-size:0.78rem;color:var(--text-muted)">${esc(loan.lenderOrBorrowerName||'')}</div></div><div style="display:flex;flex-direction:column;gap:3px;align-items:flex-end"><span class="badge ${tb}">${esc(loan.loanType)}</span><span class="badge ${sb}">${esc(loan.status)}</span></div></div>`
    }<div style="font-weight:700;margin-bottom:6px">${fmt(loan.principalAmount||0,loan.currency)}</div>${progressBar(rp,rp>=100?'progress-green':'progress-orange')}<div style="display:flex;justify-content:space-between;font-size:0.78rem;color:var(--text-muted);margin-top:3px"><span>Paid: ${fmt(loan.totalPaid||0,loan.currency)}</span><span>Left: ${fmt(loan.remainingBalance||0,loan.currency)}</span></div>${loan.interestAccrued?`<div style="font-size:0.78rem;color:var(--warning);margin-top:2px">Interest: ${fmt(loan.interestAccrued,loan.currency)}</div>`:''}</div>`;
  });
  return body + '</div>';
}

function renderCategoryPieBody(items, displayCur) {
  if (!items.length) return '<p class="empty-state">No category data.</p>';
  const cur = displayCur || cachedSettings.baseCurrency || '';
  const top10 = items.slice(0,10); const rest = items.slice(10);
  const slices = [...top10];
  if (rest.length) slices.push({ categoryName:'Other', amount:rest.reduce((s,c)=>s+(c.amount||0),0), percentage:rest.reduce((s,c)=>s+(c.percentage||0),0), transactionCount:rest.reduce((s,c)=>s+(c.transactionCount||0),0) });
  const cs = slices.map((c,i)=>({ label:c.categoryName, value:c.amount, color:c.colorHex||CHART_COLORS[i%CHART_COLORS.length] }));
  let leg = '<div class="chart-legend" style="flex:1">';
  slices.forEach((c,i)=>{ leg += `<div class="legend-item"><div class="legend-dot" style="background:${esc(c.colorHex||CHART_COLORS[i%CHART_COLORS.length])}"></div><span>${esc(c.categoryName)}</span><span style="color:var(--text-muted);margin-left:4px">${fmt(c.amount||0,cur)} (${fmtRaw(c.percentage||0,1)}%)</span>${c.transactionCount?`<span style="color:var(--text-muted);font-size:0.72rem;margin-left:4px">${c.transactionCount} txns</span>`:''}</div>`; });
  leg += '</div>';
  return `<div style="display:flex;flex-wrap:wrap;gap:20px;align-items:center">${svgDonut(cs,160)}${leg}</div>`;
}

function renderCategoryTreemapBody(items, displayCur) {
  if (!items.length) return '<p class="empty-state">No category data.</p>';
  const cur = displayCur || cachedSettings.baseCurrency || '';
  const tm = items.map((c,i)=>({ categoryName:c.categoryName, amount:c.amount, percentage:c.percentage, colorHex:c.colorHex||CHART_COLORS[i%CHART_COLORS.length] }));
  let leg = '<div class="chart-legend" style="margin-top:12px">';
  items.slice(0,10).forEach((c,i)=>{ leg += `<div class="legend-item"><div class="legend-dot" style="background:${esc(c.colorHex||CHART_COLORS[i%CHART_COLORS.length])}"></div><span>${esc(c.categoryName)}</span><span style="color:var(--text-muted);margin-left:4px">${fmt(c.amount||0,cur)}</span><span style="color:var(--text-muted);margin-left:4px">${fmtRaw(c.percentage||0,1)}%</span></div>`; });
  leg += '</div>';
  return `<div class="treemap-wrap">${svgTreemapChart(tm,560,180)}</div>${leg}`;
}

function renderIncomeExpenseBody(pts, gran, baseCur, todayIdx) {
  if (!pts.length) return '<p class="empty-state">No data.</p>';
  const points = pts.map(b=>({ label:b.label, income:b.income||0, expense:b.expense||0 }));
  const hasTodayMarker = todayIdx != null && todayIdx >= 0 && todayIdx < points.length;
  const totalIncome  = pts.reduce((s,p)=>s+(p.income||0),0);
  const totalExpense = pts.reduce((s,p)=>s+(p.expense||0),0);
  const netTotal = totalIncome - totalExpense;
  const netCls   = netTotal >= 0 ? 'positive' : 'negative';
  const summary = `<div style="display:flex;gap:16px;margin-bottom:8px;font-size:0.85rem;flex-wrap:wrap">
    <span class="amount-income">Income: +${fmt(totalIncome,baseCur)}</span>
    <span class="amount-expense">Expense: −${fmt(totalExpense,baseCur)}</span>
    <span class="${netCls}">Net: ${netTotal>=0?'+':'−'}${fmt(Math.abs(netTotal),baseCur)}</span>
  </div>`;
  return `${summary}${svgBars(points,560,140,true,hasTodayMarker?todayIdx:-1)}<div class="chart-legend" style="margin-top:8px">
    <div class="legend-item"><div class="legend-dot" style="background:var(--success)"></div><span>Income</span></div>
    <div class="legend-item"><div class="legend-dot" style="background:var(--danger)"></div><span>Expense</span></div>
    <div class="legend-item"><div class="legend-dot" style="background:var(--primary);border:1px dashed var(--primary);background:none;width:14px;height:2px;border-radius:0;border-top:2px dashed var(--primary)"></div><span>Net</span></div>
    <span style="font-size:0.78rem;color:var(--text-muted);margin-left:auto">${esc(String(gran||'').toLowerCase())}${baseCur?' · '+esc(baseCur):''}</span>
  </div>`;
}

function renderRecentActivitiesBody(txs, total) {
  if (!txs.length) return '<p class="empty-state">No transactions found.</p>';
  let body = `<div class="table-wrap"><table><thead><tr><th>Date</th><th>Title</th><th>Category</th><th>Account</th><th>Tags</th><th style="text-align:right">Amount</th></tr></thead><tbody>`;
  txs.forEach(tx => {
    const amtCls=tx.type==='INCOME'?'amount-income':tx.type==='EXPENSE'?'amount-expense':'';
    const sign=tx.type==='EXPENSE'?'−':tx.type==='INCOME'?'+':'';
    const bdg=tx.type==='INCOME'?'badge-income':tx.type==='EXPENSE'?'badge-expense':'badge-transfer';
    const tags=tx.tags&&tx.tags.length?tx.tags.map(t=>`<span class="badge" style="background:var(--surface2);color:var(--text);font-size:0.68rem">${esc(t)}</span>`).join(' '):'—';
    body+=`<tr style="cursor:pointer" onclick="openTxDetail(${tx.id})"><td style="white-space:nowrap">${fmtDate(tx.date)}</td><td><span class="badge ${bdg}" style="font-size:0.68rem;margin-right:4px">${prettyEnum(tx.type)}</span>${esc(tx.title)}${tx.note?`<br><small style="color:var(--text-muted)">${esc(tx.note)}</small>`:''}</td><td>${tx.categoryName?esc(tx.categoryName):'<span style="color:var(--text-muted)">—</span>'}</td><td>${esc(tx.accountName||'—')}</td><td>${tags}</td><td style="text-align:right" class="${amtCls}">${sign}${fmt(Math.abs(tx.amount||0),tx.currency)}</td></tr>`;
  });
  body += '</tbody></table></div>';
  if (total) body += `<div style="font-size:0.78rem;color:var(--text-muted);margin-top:8px;text-align:right">${total} total · <a href="#/records" style="color:var(--accent)">View all</a></div>`;
  return body;
}

const cashFlowComparisonMetric = {};
function renderCashFlowComparisonBody(d, cardId, metric) {
  const current=d.current||[], previous=d.previous||[], lastYear=d.lastYear||[];
  if (!current.length) return '<p class="empty-state">No comparison data.</p>';
  const m = metric || cashFlowComparisonMetric[cardId] || 'net';
  const getVal = (p,key) => key==='income'?(p.income||0):key==='expense'?(p.expense||0):(p.net||0);
  const prevByMs = Object.fromEntries(previous.map(p=>[p.epochMillis, getVal(p,m)]));
  const lyByMs   = Object.fromEntries(lastYear.map(p=>[p.epochMillis, getVal(p,m)]));
  const series = [
    { name:'Current',   color:'#1976D2', opacity:1,    labels:current.map(p=>p.label||''), points:current.map(p=>getVal(p,m)) },
    { name:'Previous',  color:'#FF9800', opacity:0.7,  labels:current.map(p=>p.label||''), points:current.map(p=>prevByMs[p.epochMillis]??0) },
    { name:'Last Year', color:'#4CAF50', opacity:0.6,  labels:current.map(p=>p.label||''), points:current.map(p=>lyByMs[p.epochMillis]??0) },
  ].filter(s=>s.points.some(v=>v!==0));
  if (!series.length) return '<p class="empty-state">No comparison data.</p>';
  const toggleHtml = cardId ? `<div style="display:flex;gap:6px;margin-bottom:10px">
    ${['net','income','expense'].map(opt=>`<button class="btn btn-sm ${m===opt?'btn-primary':'btn-ghost'}" onclick="(function(){cashFlowComparisonMetric['${esc(cardId)}']='${opt}';document.getElementById('card-body-${esc(cardId)}').innerHTML=renderCashFlowComparisonBody(cachedCfcData&&cachedCfcData['${esc(cardId)}']||{current:[],previous:[],lastYear:[]},'${esc(cardId)}','${opt}');})()">${opt.charAt(0).toUpperCase()+opt.slice(1)}</button>`).join('')}
  </div>` : '';
  return `${toggleHtml}${svgLineChart(series,560,160)}<div class="chart-legend" style="margin-top:10px">${series.map(s=>`<div class="legend-item"><div class="legend-dot" style="background:${esc(s.color)}"></div><span>${esc(s.name)}</span></div>`).join('')}</div>`;
}

function renderCategorySpendingTrendBody(d) {
  const buckets = d.buckets||[];
  if (!buckets.length) return '<p class="empty-state">No spending trend data.</p>';
  const allCats = [...new Set(buckets.flatMap(b=>(b.categories||[]).map(c=>c.categoryName)))];
  const colorMap = {}; allCats.forEach((name,i)=>{ colorMap[name]=CHART_COLORS[i%CHART_COLORS.length]; });
  const catTotals = {};
  buckets.forEach(b=>(b.categories||[]).forEach(c=>{ catTotals[c.categoryName]=(catTotals[c.categoryName]||0)+Math.abs(c.amount||0); }));
  const catNature = {};
  buckets.forEach(b=>(b.categories||[]).forEach(c=>{ if(c.nature) catNature[c.categoryName]=c.nature; }));
  const topCats = Object.entries(catTotals).sort((a,b)=>b[1]-a[1]).slice(0,8);
  let legend = '<div class="chart-legend" style="margin-top:12px">';
  topCats.forEach(([name,total])=>{
    const nature = catNature[name] || '';
    const natureCls = nature==='MUST'?'badge-must':nature==='NEED'?'badge-need':nature==='WANT'?'badge-want':'';
    legend+=`<div class="legend-item"><div class="legend-dot" style="background:${esc(colorMap[name]||'#9e9e9e')}"></div><span>${esc(name)}</span>${nature?`<span class="badge ${natureCls}" style="font-size:0.68rem;margin-left:4px">${esc(nature)}</span>`:''}<span style="color:var(--text-muted);margin-left:4px">${fmtRaw(total)}</span></div>`;
  });
  legend += '</div>';
  return `${svgStackedBarsChart(buckets,colorMap,560,140)}${legend}`;
}

function renderTagUpSetPlotBody(d, baseCur) {
  const sets=d.sets||[], inters=d.intersections||[];
  const cur = baseCur || cachedSettings.baseCurrency || '';
  if (!sets.length) return '<p class="empty-state">No tag data.</p>';
  const maxAmt = Math.max(...sets.map(s=>Math.abs(s.totalAmount||0)),1);
  let body = '<div style="margin-bottom:16px">';
  sets.forEach(s => {
    const pct = Math.min(100, Math.round(Math.abs(s.totalAmount||0)/maxAmt*100));
    const isInc = (s.totalAmount||0) >= 0;
    body += `<div style="margin-bottom:8px"><div style="display:flex;justify-content:space-between;align-items:center;font-size:0.85rem;margin-bottom:3px;gap:8px"><span style="font-weight:600;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${esc(s.tagName)}</span><span class="amount-income" style="font-size:0.78rem">+${fmt(s.income||0,cur)}</span><span class="amount-expense" style="font-size:0.78rem">−${fmt(s.expense||0,cur)}</span><span style="color:var(--text-muted);font-size:0.78rem;white-space:nowrap">${s.transactionCount||0} txns</span></div><div class="progress-track"><div class="progress-fill ${isInc?'progress-green':'progress-orange'}" style="width:${pct}%"></div></div></div>`;
  });
  body += '</div>';
  if (inters.length) {
    body += `<div style="font-size:0.85rem;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:.04em;margin-bottom:8px">Top Intersections</div><div class="table-wrap"><table><thead><tr><th>Tags</th><th style="text-align:right">Amount</th><th style="text-align:right">Income</th><th style="text-align:right">Expense</th><th>Txns</th></tr></thead><tbody>`;
    inters.slice(0,10).forEach(inter => {
      const bdgs=(inter.tags||[]).map(t=>`<span class="badge" style="background:var(--surface2);color:var(--text);font-size:0.7rem">${esc(t)}</span>`).join(' ');
      body+=`<tr><td>${bdgs}</td><td style="text-align:right">${fmt(Math.abs(inter.amount||0),cur)}</td><td style="text-align:right" class="amount-income">+${fmt(inter.income||0,cur)}</td><td style="text-align:right" class="amount-expense">−${fmt(inter.expense||0,cur)}</td><td>${inter.transactionCount||0}</td></tr>`;
    });
    body += '</tbody></table></div>';
  }
  return body;
}

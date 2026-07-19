/* PAGE: Budgets + Budget Detail */

function _budgetAlertColor(alertLevel) {
  if (alertLevel === 'EXCEEDED') return 'var(--expense)';
  if (alertLevel === 'WARNING')  return 'var(--warning)';
  return 'var(--income)';
}

function _budgetPeriodLabel(b) {
  const map = { MONTHLY: 'Monthly', YEARLY: 'Yearly', WEEKLY: 'Weekly', TODAY: 'Daily', QUARTER: 'Quarterly' };
  return map[b.timelineType] || b.period || '';
}

function _budgetProgressRow(b) {
  const pct      = Math.round(b.percentage || 0);
  const clampPct = Math.min(100, pct);
  const color    = _budgetAlertColor(b.alertLevel);
  return `<div class="bud-item-progress-wrap">
    <div class="bud-item-progress-track">
      <div class="bud-item-progress-fill" style="width:${clampPct}%;background:${color}"></div>
    </div>
    <div class="bud-item-progress-row">
      <span class="bud-item-stat">${fmt(b.spent||0, b.currency)} spent</span>
      <span class="bud-item-stat" style="color:${color};font-weight:600">${pct}%</span>
    </div>
  </div>`;
}

function _budgetListItem(b, showArchived) {
  const pct       = Math.round(b.percentage || 0);
  const color     = _budgetAlertColor(b.alertLevel);
  const period    = _budgetPeriodLabel(b);
  const isOver    = b.alertLevel === 'EXCEEDED';
  const isWarn    = b.alertLevel === 'WARNING';

  const iconSvg = isOver
    ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`
    : isWarn
    ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`
    : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`;

  const archiveBtn = b.isArchived
    ? `<button class="bud-menu-btn" onclick="event.stopPropagation();restoreBudget('${esc(b.id)}','${esc(b.name)}')">Restore</button>`
    : `<button class="bud-menu-btn" onclick="event.stopPropagation();archiveBudget('${esc(b.id)}','${esc(b.name)}')">Archive</button>`;

  return `<div class="bud-item" onclick="navigate('#/budgets/${esc(b.id)}')">
    <div class="bud-item-icon" style="background:${color}1a;color:${color}">${iconSvg}</div>
    <div class="bud-item-body">
      <div class="bud-item-header">
        <div>
          <div class="bud-item-name">${esc(b.name)}</div>
          <div class="bud-item-sub">${esc(period)}${b.isArchived ? ' · Archived' : ''}</div>
        </div>
        <div class="bud-item-trail">
          <div class="bud-item-limit">${fmt(b.limitAmount||0, b.currency)}</div>
          <div class="bud-item-trail-pct" style="color:${color}">${pct}%</div>
        </div>
      </div>
      ${_budgetProgressRow(b)}
    </div>
    <div class="bud-item-actions" onclick="event.stopPropagation()">
      <button class="bud-menu-btn" onclick="openEditBudget('${esc(b.id)}')">Edit</button>
      ${archiveBtn}
      <button class="bud-menu-btn danger" onclick="deleteBudget('${esc(b.id)}','${esc(b.name)}')">Delete</button>
    </div>
  </div>`;
}

async function renderBudgets() {
  const params       = new URLSearchParams(location.hash.split('?')[1] || '');
  const showArchived = params.get('archived') === '1';

  showSpinner();
  try {
    const url  = showArchived ? '/api/budgets?includeArchived=true' : '/api/budgets';
    const json = await apiFetch(url);
    if (!json.success) { showError('Failed to load budgets.'); return; }
    const budgets  = json.data || [];
    const active   = budgets.filter(b => !b.isArchived);
    const archived = budgets.filter(b =>  b.isArchived);

    let html = `<div class="page-header">
      <h2 class="section-title">Budgets</h2>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <button class="btn btn-ghost btn-sm${showArchived?' btn-filter-active':''}" onclick="toggleBudgetArchived()">
          ${showArchived ? 'Hide Archived' : `Archived (${archived.length})`}
        </button>
        <button class="btn btn-primary btn-sm" onclick="openAddBudget()">+ Add Budget</button>
      </div>
    </div>`;

    if (!active.length && !showArchived) {
      html += emptyState('', 'No budgets yet.', '+ Add Budget', 'openAddBudget()');
    } else {
      html += '<div class="bud-list">';

      if (active.length) {
        html += `<div class="bud-section-header">ACTIVE</div>`;
        active.forEach(b => { html += _budgetListItem(b, false); });
      }

      if (showArchived && archived.length) {
        html += `<div class="bud-section-header" style="margin-top:20px">ARCHIVED</div>`;
        archived.forEach(b => { html += _budgetListItem(b, true); });
      } else if (showArchived && !archived.length) {
        html += `<div class="bud-section-header">ARCHIVED</div>`;
        html += `<p style="color:var(--text-muted);font-size:0.875rem;padding:12px 0">No archived budgets.</p>`;
      }

      html += '</div>';
    }

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading budgets.'); }
}

window.toggleBudgetArchived = function () {
  const params = new URLSearchParams(location.hash.split('?')[1] || '');
  const cur = params.get('archived') === '1';
  if (cur) params.delete('archived'); else params.set('archived', '1');
  const qs = params.toString();
  location.hash = '#/budgets' + (qs ? '?' + qs : '');
};

window.archiveBudget = async function (id, name) {
  try {
    const res = await apiWrite('PUT', `/api/budgets/${encodeURIComponent(id)}`, { isArchived: true });
    if (res.success) { renderBudgets(); showToast(`"${name}" archived.`); }
    else { showToast('Failed to archive budget.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error archiving budget.', 'error'); }
};

window.restoreBudget = async function (id, name) {
  try {
    const res = await apiWrite('PUT', `/api/budgets/${encodeURIComponent(id)}`, { isArchived: false });
    if (res.success) { renderBudgets(); showToast(`"${name}" restored.`); }
    else { showToast('Failed to restore budget.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error restoring budget.', 'error'); }
};

window.openAddBudget = function () {
  const body = `
    ${formField('Budget Name', '<input type="text" id="bud-name" placeholder="e.g. Groceries">')}
    ${formRow(
      formField('Currency', `<input type="text" id="bud-currency" value="${esc(cachedSettings.baseCurrency||'USD')}" maxlength="5" oninput="const p=document.getElementById('bud-limit-pfx');if(p)p.textContent=this.value.toUpperCase()||'—'">`),
      formField('Limit Amount', `<div class="input-group"><span class="input-group-addon" id="bud-limit-pfx">${esc(cachedSettings.baseCurrency||'USD')}</span><input type="number" id="bud-limit" placeholder="500.00" min="0" step="0.01"></div>`)
    )}
    ${formRow(
      formField('Period', buildSelect('bud-period',[{value:'MONTHLY',label:'Monthly'},{value:'WEEKLY',label:'Weekly'},{value:'YEARLY',label:'Yearly'},{value:'TODAY',label:'Daily'},{value:'QUARTER',label:'Quarterly'}],'MONTHLY','— Select —',false)),
      `<div class="form-field"><label data-tip="Alert when spending reaches this % of the limit">Alert Threshold (%)</label><div class="input-group"><input type="number" id="bud-alert" value="80" min="0" max="100" step="1" placeholder="80"><span class="input-group-addon">%</span></div></div>`
    )}
    ${cachedCategories.length ? `<div class="form-field"><label>Track Categories <small style="color:var(--text-muted);font-weight:400">(leave empty to track all)</small></label>${chipCheckboxes('bud-cats', cachedCategories.map(c=>({value:String(c.id),label:c.name})), [])}</div>` : ''}`;
  openModal('Add Budget', body, submitAddBudget, 'Save Budget');
  modalConfBtn.className = 'btn btn-primary';
};

window.submitAddBudget = async function () {
  clearFieldErrors(modalBody);
  const name     = document.getElementById('bud-name')?.value.trim();
  const currency = document.getElementById('bud-currency')?.value.trim();
  const limit    = parseFloat(document.getElementById('bud-limit')?.value || '0');
  const period   = document.getElementById('bud-period')?.value;
  const alertPct = parseFloat(document.getElementById('bud-alert')?.value || '80');
  const catIds   = getCheckedValues('bud-cats').map(Number).filter(Boolean);
  if (!name)                    { showFieldError('bud-name', 'Name is required.'); return; }
  if (isNaN(limit) || limit <= 0) { showFieldError('bud-limit', 'Enter a valid limit amount.'); return; }
  await withLoading(modalConfBtn, async () => {
    const payload = { name, currency, limitAmount: limit, timelineType: period, alertThreshold: alertPct / 100 };
    if (catIds.length) payload.categoryIds = catIds;
    const res = await apiWrite('POST', '/api/budgets', payload);
    if (res.success) { closeModal(); renderBudgets(); showToast('Budget created!'); }
    else { showToast('Failed to create budget: ' + (res.error || ''), 'error'); }
  });
};

window.openEditBudget = async function (id) {
  const resp = await apiFetch(`/api/budgets/${encodeURIComponent(id)}`);
  if (!resp.success) { showToast('Could not load budget.', 'error'); return; }
  const b = resp.data;
  const body = `
    ${formField('Budget Name', `<input type="text" id="bud-name" value="${esc(b.name)}">`)}
    ${formRow(
      formField('Currency', `<input type="text" id="bud-currency" value="${esc(b.currency)}" maxlength="5" oninput="const p=document.getElementById('bud-limit-pfx');if(p)p.textContent=this.value.toUpperCase()||'—'">`),
      formField('Limit Amount', `<div class="input-group"><span class="input-group-addon" id="bud-limit-pfx">${esc(b.currency)}</span><input type="number" id="bud-limit" value="${esc(b.limitAmount||0)}" min="0" step="0.01"></div>`)
    )}
    ${formRow(
      formField('Period', buildSelect('bud-period',[{value:'MONTHLY',label:'Monthly'},{value:'WEEKLY',label:'Weekly'},{value:'YEARLY',label:'Yearly'},{value:'TODAY',label:'Daily'},{value:'QUARTER',label:'Quarterly'}],b.timelineType||'MONTHLY','— Select —',false)),
      `<div class="form-field"><label>Alert Threshold (%)</label><div class="input-group"><input type="number" id="bud-alert" value="${esc(b.alertThreshold!=null ? Math.round((b.alertThreshold||0.8)*100) : 80)}" min="0" max="100" step="1"><span class="input-group-addon">%</span></div></div>`
    )}
    <div class="form-field"><label>Archive</label>
      <div class="toggle-wrap"><input type="checkbox" id="bud-archived" ${b.isArchived?'checked':''}><span style="font-size:0.88rem">Hide from active budgets</span></div>
    </div>`;
  openModal(`Edit Budget — ${esc(b.name)}`, body, async () => {
    clearFieldErrors(modalBody);
    const name     = document.getElementById('bud-name')?.value.trim();
    const curr     = document.getElementById('bud-currency')?.value.trim();
    const limit    = parseFloat(document.getElementById('bud-limit')?.value || '0');
    const period   = document.getElementById('bud-period')?.value;
    const alertPct = parseFloat(document.getElementById('bud-alert')?.value || '80');
    const isArchived = document.getElementById('bud-archived')?.checked || false;
    if (!name)                    { showFieldError('bud-name', 'Name is required.'); return; }
    if (isNaN(limit) || limit <= 0) { showFieldError('bud-limit', 'Enter a valid limit.'); return; }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/budgets/${encodeURIComponent(id)}`, {
        name, currency: curr, limitAmount: limit, timelineType: period, alertThreshold: alertPct / 100, isArchived
      });
      if (res.success) { closeModal(); renderBudgets(); showToast('Budget updated!'); }
      else { showToast('Failed to update: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
};

window.deleteBudget = function (id, name) {
  confirmDelete(`Delete budget "${name}"?`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/budgets/${encodeURIComponent(id)}`);
      closeModal();
      if (res.success) { renderBudgets(); showToast('Budget deleted.'); }
      else { showToast('Failed to delete budget.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting budget.', 'error'); }
  });
};

function _budgetPeriodRange(b) {
  const parts = (b.period || '').split('-').map(Number);
  const year  = parts[0] || new Date().getFullYear();
  const month = parts[1] || (new Date().getMonth() + 1);
  switch (b.timelineType) {
    case 'MONTHLY': return {
      from: new Date(year, month - 1, 1).getTime(),
      to:   new Date(year, month, 0, 23, 59, 59, 999).getTime()
    };
    case 'YEARLY': return {
      from: new Date(year, 0, 1).getTime(),
      to:   new Date(year, 11, 31, 23, 59, 59, 999).getTime()
    };
    case 'WEEKLY': {
      const now = new Date(); const day = now.getDay();
      const mon = new Date(now); mon.setDate(now.getDate() - ((day + 6) % 7)); mon.setHours(0,0,0,0);
      const sun = new Date(mon); sun.setDate(mon.getDate() + 6); sun.setHours(23,59,59,999);
      return { from: mon.getTime(), to: sun.getTime() };
    }
    case 'TODAY': {
      const s = new Date(); s.setHours(0,0,0,0);
      const e = new Date(); e.setHours(23,59,59,999);
      return { from: s.getTime(), to: e.getTime() };
    }
    case 'QUARTER': {
      const q = Math.floor((month - 1) / 3);
      return {
        from: new Date(year, q * 3, 1).getTime(),
        to:   new Date(year, q * 3 + 3, 0, 23, 59, 59, 999).getTime()
      };
    }
    default: return {
      from: new Date(year, month - 1, 1).getTime(),
      to:   new Date(year, month, 0, 23, 59, 59, 999).getTime()
    };
  }
}

function _budgetHeroHtml(b) {
  const pct       = Math.round(b.percentage || 0);
  const clampPct  = Math.min(100, pct);
  const isOver    = b.alertLevel === 'EXCEEDED';
  const isWarn    = b.alertLevel === 'WARNING';
  const color     = _budgetAlertColor(b.alertLevel);
  const period    = _budgetPeriodLabel(b);
  const threshPct = Math.round((b.alertThreshold || 0.8) * 100);

  const alertBanner = (isOver || isWarn) ? `<div class="bud-hero-banner" style="border-color:${color}40;background:${color}0d">
    <div class="bud-hero-banner-icon" style="background:${color}1a;color:${color}">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
        ${isOver
          ? '<path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>'
          : '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>'}
      </svg>
    </div>
    <div class="bud-hero-banner-text">
      <div class="bud-hero-banner-title" style="color:${color}">${isOver ? 'BUDGET EXCEEDED' : 'APPROACHING LIMIT'}</div>
      <div class="bud-hero-banner-sub">${isOver
        ? `Spending exceeds the limit by ${fmt(Math.abs(b.remaining||0), b.currency)}`
        : `Alert threshold set at ${threshPct}%`}</div>
    </div>
  </div>` : '';

  return `<div class="bud-hero">
    <div class="bud-hero-title-row">
      <div class="bud-hero-icon-wrap" style="background:${color}1a;color:${color}">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
      </div>
      <div>
        <div class="bud-hero-card-title">${esc(b.name)}</div>
        <div class="bud-hero-card-sub">Budget: ${esc(period)}</div>
      </div>
    </div>
    <div class="bud-hero-top">
      <div class="bud-hero-meta">
        <div class="bud-hero-label">${isOver ? 'OVER BUDGET' : 'REMAINING'}</div>
        <div class="bud-hero-amount" style="color:${isOver ? 'var(--expense)' : 'var(--text)'}">
          ${isOver ? '−' : ''}${fmt(Math.abs(b.remaining||0), b.currency)}
        </div>
      </div>
      <div class="bud-hero-badge" style="background:${color}1a;color:${color};border:1px solid ${color}30">${pct}%</div>
    </div>
    <div class="bud-hero-track">
      <div class="bud-hero-fill" style="width:${clampPct}%;background:${color}"></div>
    </div>
    <div class="bud-hero-stats">
      <span class="bud-hero-stat-spent"><span class="bud-hero-dot" style="background:${color}"></span>${fmt(b.spent||0, b.currency)} spent</span>
      <span class="bud-hero-stat-limit">Limit: ${fmt(b.limitAmount||0, b.currency)}</span>
    </div>
    ${alertBanner}
  </div>`;
}

function _budgetScopeHtml(b) {
  const periodDisplay = { MONTHLY: 'This Month', YEARLY: 'This Year', WEEKLY: 'This Week', TODAY: 'Today', QUARTER: 'This Quarter' };
  const periodLabel   = periodDisplay[b.timelineType] || _budgetPeriodLabel(b);

  const catIds  = b.categoryIds  || [];
  const accIds  = b.accountIds   || [];
  const cats    = catIds.length  ? cachedCategories.filter(c => catIds.includes(c.id))   : [];
  const accs    = accIds.length  ? cachedAccounts.filter(a => accIds.includes(a.id))     : [];

  function scopeRow(icon, label, chips, allText) {
    const chipsHtml = chips.length
      ? chips.map(c => `<span class="bud-scope-chip">${esc(c.name || c)}</span>`).join('')
      : `<span class="bud-scope-chip bud-scope-chip-all">${esc(allText)}</span>`;
    return `<div class="bud-scope-row">
      <div class="bud-scope-row-label">
        <span class="bud-scope-icon">${icon}</span>
        <span class="bud-scope-key">${label}</span>
      </div>
      <div class="bud-scope-chips">${chipsHtml}</div>
    </div>`;
  }

  const calIcon = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>`;
  const catIcon = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>`;
  const accIcon = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/></svg>`;

  let html = `<div class="bud-scope-card">
    <div class="bud-scope-header">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
      TRACKING SCOPE
    </div>`;
  html += scopeRow(calIcon, 'PERIOD',     [{name: periodLabel}],           '—');
  html += scopeRow(catIcon, 'CATEGORIES', cats,                            'All Categories');
  html += scopeRow(accIcon, 'ACCOUNTS',   accs,                            'All Accounts');
  html += '</div>';

  if (b.rollover) {
    html += `<div class="bud-rollover-banner">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
      <div>
        <div class="bud-rollover-title">Rollover Applied</div>
        <div class="bud-rollover-sub">Unused budget from the previous period carries over to this month.</div>
      </div>
    </div>`;
  }

  return html;
}

function _budgetActivityHtml(txs, b) {
  if (!txs.length) {
    return `<div class="bud-activity-empty">
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M8 12h8M12 8v8"/></svg>
      <div>No transactions in this budget period.</div>
    </div>`;
  }

  // Group by date
  const groups = {};
  txs.forEach(tx => {
    const d   = new Date(tx.date);
    const key = d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    if (!groups[key]) groups[key] = [];
    groups[key].push(tx);
  });

  let html = '<div class="bud-activity-list">';
  Object.entries(groups).forEach(([label, items]) => {
    html += `<div class="bud-activity-group-label">${esc(label)}</div>`;
    items.forEach(tx => {
      const isInc = tx.type === 'INCOME';
      const isExp = tx.type === 'EXPENSE';
      const cls   = isInc ? 'income' : (isExp ? 'expense' : 'transfer');
      const sign  = isInc ? '+' : (isExp ? '−' : '');
      html += `<div class="bud-activity-row" onclick="openTxDetail && openTxDetail(${tx.id})">
        <div class="tx-dot ${cls}"></div>
        <div class="bud-activity-info">
          <div class="bud-activity-name">${esc(tx.title)}</div>
          ${tx.categoryName ? `<div class="bud-activity-sub">${esc(tx.categoryName)}${tx.accountName ? ' · ' + esc(tx.accountName) : ''}</div>` : ''}
        </div>
        <div class="bud-activity-amount ${cls}">${sign}${fmt(tx.amount, tx.currency)}</div>
      </div>`;
    });
  });
  html += '</div>';
  return html;
}

window.budDetailTab = function(tab) {
  document.querySelectorAll('.bud-detail-tab').forEach(el => el.classList.toggle('active', el.dataset.tab === tab));
  document.querySelectorAll('.bud-detail-pane').forEach(el => el.classList.toggle('active', el.dataset.tab === tab));
};

async function renderBudgetDetail(id) {
  if (!id) { navigate('#/budgets'); return; }
  showSpinner();
  try {
    const [budJson, txJson] = await Promise.all([
      apiFetch(`/api/budgets/${encodeURIComponent(id)}`),
      apiFetch('/api/transactions?limit=200')
    ]);
    if (!budJson.success) { showError('Failed to load budget.'); return; }
    const b   = budJson.data || {};
    const allTxs = txJson.data?.items || [];

    // Filter transactions to this budget's period and scope
    const { from, to } = _budgetPeriodRange(b);
    let txs = allTxs.filter(t => t.date >= from && t.date <= to);
    if ((b.categoryIds||[]).length) txs = txs.filter(t => (b.categoryIds).includes(t.categoryId));
    if ((b.accountIds||[]).length)  txs = txs.filter(t => (b.accountIds).includes(t.accountId));

    const heroHtml     = _budgetHeroHtml(b);
    const scopeHtml    = _budgetScopeHtml(b);
    const activityHtml = _budgetActivityHtml(txs, b);

    const archivedBadge = b.isArchived ? ' <span class="badge" style="background:var(--border);color:var(--text-muted);font-size:0.65rem;vertical-align:middle;margin-left:4px">Archived</span>' : '';

    const html = `<div class="page-header">
      <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
        <a class="btn btn-ghost btn-sm" href="#/budgets">← Back</a>
        <div class="bud-detail-pill-nav">
          <span>${esc(b.name)}${archivedBadge}</span>
          <div class="bud-detail-tabs">
            <button class="bud-detail-tab active" data-tab="overview" onclick="budDetailTab('overview')">Overview</button>
            <button class="bud-detail-tab" data-tab="activity" onclick="budDetailTab('activity')">Activity</button>
          </div>
        </div>
      </div>
      <div style="display:flex;gap:6px">
        <button class="btn btn-ghost btn-sm" onclick="openEditBudget('${esc(b.id)}')">Edit</button>
        ${b.isArchived
          ? `<button class="btn btn-ghost btn-sm" onclick="restoreBudget('${esc(b.id)}','${esc(b.name)}')">Restore</button>`
          : `<button class="btn btn-ghost btn-sm" onclick="archiveBudget('${esc(b.id)}','${esc(b.name)}')">Archive</button>`}
      </div>
    </div>

    <div class="bud-detail-pane active" data-tab="overview">
      ${heroHtml}
      <div style="margin-top:16px">${scopeHtml}</div>
    </div>

    <div class="bud-detail-pane" data-tab="activity">
      <div class="bud-activity-header">
        <span>${txs.length} transaction${txs.length !== 1 ? 's' : ''} in this period</span>
      </div>
      ${activityHtml}
    </div>`;

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading budget detail.'); }
}

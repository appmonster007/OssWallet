/* PAGE: Budgets + Budget Detail */

async function renderBudgets() {
  const params       = new URLSearchParams(location.hash.split('?')[1] || '');
  const showArchived = params.get('archived') === '1';

  showSpinner();
  try {
    const url = showArchived ? '/api/budgets?includeArchived=true' : '/api/budgets';
    const json = await apiFetch(url);
    if (!json.success) { showError('Failed to load budgets.'); return; }
    const budgets = json.data || [];
    const active   = budgets.filter(b => !b.isArchived);
    const archived = budgets.filter(b =>  b.isArchived);
    const displayed = showArchived ? archived : active;

    let html = `<div class="page-header"><h2 class="section-title">Budgets</h2>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <button class="btn btn-ghost btn-sm${showArchived?' btn-filter-active':''}" onclick="toggleBudgetArchived()" title="Show/hide archived budgets">
          📦 ${showArchived ? 'Hide Archived' : `Archived (${archived.length})`}
        </button>
        <button class="btn btn-primary btn-sm" onclick="openAddBudget()">+ Add Budget</button>
      </div>
    </div>`;

    if (!displayed.length) {
      html += showArchived
        ? emptyState('📦', 'No archived budgets.', 'Show Active', 'toggleBudgetArchived()')
        : emptyState('📈', 'No budgets yet.', '+ Add Budget', 'openAddBudget()');
    } else {
      html += '<div class="card-grid">';
      displayed.forEach(b => {
        const pct       = Math.round(((b.spent || 0) / (b.limitAmount || 1)) * 100);
        const alertCls  = pct < 80 ? 'progress-green' : pct < 100 ? 'progress-orange' : 'progress-red';
        html += `<div class="stat-card">
          <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:8px">
            <div>
              <div style="font-weight:700">${esc(b.name)}</div>
              <div style="font-size:0.78rem;color:var(--text-muted)">${esc(b.period||'')}</div>
            </div>
            <span style="font-size:1rem;font-weight:700;color:${pct>=100?'var(--danger)':pct>=80?'var(--warning)':'var(--success)'}">${pct}%</span>
          </div>
          ${progressBar(pct, alertCls)}
          <div style="display:flex;justify-content:space-between;font-size:0.8rem;color:var(--text-muted);margin-top:6px">
            <span>Spent: ${fmt(b.spent||0,b.currency)}</span>
            <span>Limit: ${fmt(b.limitAmount||0,b.currency)}</span>
          </div>
          <div style="font-size:0.8rem;color:var(--text-muted);margin-top:2px">Remaining: ${fmt(b.remaining||0,b.currency)}</div>
          <div style="margin-top:12px;display:flex;gap:6px;flex-wrap:wrap">
            ${!b.isArchived ? `<a class="btn btn-primary btn-sm" href="#/budgets/${esc(b.id)}">Details</a>` : ''}
            <button class="btn btn-ghost btn-sm" onclick="openEditBudget('${esc(b.id)}')">✏️ Edit</button>
            ${b.isArchived
              ? `<button class="btn btn-ghost btn-sm" onclick="restoreBudget('${esc(b.id)}','${esc(b.name)}')">♻️ Restore</button>`
              : `<button class="btn btn-ghost btn-sm" onclick="archiveBudget('${esc(b.id)}','${esc(b.name)}')">📦 Archive</button>`
            }
            <button class="btn btn-sm" style="background:var(--danger);color:#fff" onclick="deleteBudget('${esc(b.id)}','${esc(b.name)}')">Delete</button>
          </div>
        </div>`;
      });
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

async function renderBudgetDetail(id) {
  if (!id) { navigate('#/budgets'); return; }
  showSpinner();
  try {
    const json = await apiFetch(`/api/budgets/${encodeURIComponent(id)}`);
    if (!json.success) { showError('Failed to load budget.'); return; }
    const b = json.data || {};
    const pct = Math.round(((b.spent||0)/(b.limitAmount||1))*100);
    const alertCls = pct < 80 ? 'progress-green' : pct < 100 ? 'progress-orange' : 'progress-red';
    const overBudget = pct >= 100;
    const nearLimit  = pct >= 80 && pct < 100;

    const html = `<div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <a class="btn btn-ghost btn-sm" href="#/budgets">← Back</a>
        <h2 class="section-title" style="margin:0">${esc(b.name)}</h2>
        ${b.isArchived ? '<span class="badge" style="background:var(--border);color:var(--text-muted)">Archived</span>' : ''}
      </div>
      <div style="display:flex;gap:8px">
        <button class="btn btn-ghost btn-sm" onclick="openEditBudget('${esc(b.id)}')">✏️ Edit</button>
      </div>
    </div>
    ${overBudget ? '<div class="alert alert-error">⚠️ Over budget! Spending has exceeded the limit.</div>' : ''}
    ${nearLimit  ? '<div class="alert alert-warn">⚠️ Approaching budget limit.</div>' : ''}
    <div class="card" style="max-width:560px">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:12px">
        <div style="font-size:0.8rem;color:var(--text-muted)">${esc(b.period||'')}</div>
        <span style="font-size:1.6rem;font-weight:700;color:${pct>=100?'var(--danger)':pct>=80?'var(--warning)':'var(--success)'}">${pct}%</span>
      </div>
      ${progressBar(pct, alertCls)}
      <div class="card-grid" style="margin-top:16px;grid-template-columns:repeat(3,1fr)">
        <div class="stat-card">
          <div class="stat-label">Limit</div>
          <div class="stat-value" style="font-size:1.1rem">${fmt(b.limitAmount||0, b.currency)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Spent</div>
          <div class="stat-value" style="font-size:1.1rem;color:var(--danger)">${fmt(b.spent||0, b.currency)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Remaining</div>
          <div class="stat-value" style="font-size:1.1rem;color:${(b.remaining||0)<0?'var(--danger)':'var(--success)'}">${fmt(b.remaining||0, b.currency)}</div>
        </div>
      </div>
      <div style="margin-top:16px;display:flex;gap:8px;flex-wrap:wrap">
        <a class="btn btn-ghost btn-sm" href="#/analytics">📉 View in Analytics</a>
        ${b.isArchived
          ? `<button class="btn btn-ghost btn-sm" onclick="restoreBudget('${esc(b.id)}','${esc(b.name)}')">♻️ Restore</button>`
          : `<button class="btn btn-ghost btn-sm" onclick="archiveBudget('${esc(b.id)}','${esc(b.name)}')">📦 Archive</button>`
        }
      </div>
    </div>`;
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading budget detail.'); }
}

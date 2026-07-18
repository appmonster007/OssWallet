/* PAGE: Saved Filters */

async function renderFilters() {
  showSpinner();
  try {
    const json = await apiFetch('/api/filters');
    if (!json.success) { showError('Failed to load filters.'); return; }
    const filters = json.data || [];

    let html = `<div class="page-header"><h2 class="section-title">Saved Filters</h2>
      <button class="btn btn-primary btn-sm" onclick="openCreateFilter()">+ New Filter</button>
    </div>
    <p style="color:var(--text-muted);font-size:0.88rem;margin-bottom:16px">Save frequently-used filter combinations and apply them to your Records in one click.</p>`;

    if (!filters.length) {
      html += emptyState('', 'No saved filters yet.', '+ New Filter', 'openCreateFilter()');
    } else {
      html += '<div class="card">';
      filters.forEach((f, idx) => {
        const summary = buildFilterSummary(f);
        html += `<div style="display:flex;align-items:flex-start;padding:12px 0;border-bottom:${idx < filters.length - 1 ? '1px solid var(--border)' : 'none'}">
          <div style="flex:1;min-width:0">
            <div style="font-weight:600;margin-bottom:4px">${esc(f.name)}</div>
            <div style="font-size:0.8rem;color:var(--text-muted);line-height:1.5">${summary || '<em>No criteria set</em>'}</div>
          </div>
          <div style="display:flex;gap:6px;flex-shrink:0;margin-left:12px;align-items:center">
            <button class="btn btn-primary btn-sm" onclick="applyFilter(${f.id})" title="Apply this filter to Records">▶ Apply</button>
            <button class="btn-icon" title="Edit" onclick="openEditFilter(${f.id})">Edit</button>
            <button class="btn-icon" title="Delete" onclick="deleteFilter(${f.id},'${esc(f.name)}')">Del</button>
          </div>
        </div>`;
      });
      html += '</div>';
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading filters.'); }
}

function buildFilterSummary(f) {
  const parts = [];
  if (f.timelineType && f.timelineType !== 'ALL_TIME') parts.push(`Period: ${f.timelineType.replace(/_/g,' ')}`);
  if (f.dateFrom) parts.push(`From: ${new Date(f.dateFrom).toLocaleDateString()}`);
  if (f.dateTo) parts.push(`To: ${new Date(f.dateTo).toLocaleDateString()}`);
  if (f.transactionTypes?.length) parts.push(`Type: ${f.transactionTypes.join(', ')}`);
  if (f.categoryIds?.length) {
    const names = f.categoryIds.map(id => {
      const c = (cachedCategories||[]).find(x => x.id === id);
      return c ? c.name : `#${id}`;
    });
    parts.push(`Categories: ${names.join(', ')}`);
  }
  if (f.accountIds?.length) {
    const names = f.accountIds.map(id => {
      const a = (cachedAccounts||[]).find(x => x.id === id);
      return a ? a.name : id;
    });
    parts.push(`Accounts: ${names.join(', ')}`);
  }
  if (f.tags?.length) parts.push(`Tags: ${f.tags.join(', ')}`);
  if (f.paymentMethods?.length) parts.push(`Payment: ${f.paymentMethods.join(', ')}`);
  if (f.statuses?.length) parts.push(`Status: ${f.statuses.join(', ')}`);
  if (f.currencies?.length) parts.push(`Currency: ${f.currencies.join(', ')}`);
  if (f.minAmount != null || f.maxAmount != null) {
    const range = [f.minAmount != null ? `≥${f.minAmount}` : null, f.maxAmount != null ? `≤${f.maxAmount}` : null].filter(Boolean);
    parts.push(`Amount: ${range.join(' ')}`);
  }
  if (f.queryNote) parts.push(`Note: "${f.queryNote}"${f.isNoteRegex ? ' (regex)' : ''}`);
  if (f.queryPayee) parts.push(`Payee: "${f.queryPayee}"${f.isPayeeRegex ? ' (regex)' : ''}`);
  if (f.hasWarranty === true) parts.push('Has warranty');
  if (f.excludeFuture === false) parts.push('Include future');
  return parts.join(' · ');
}

window.applyFilter = async function (id) {
  try {
    const res = await apiFetch(`/api/filters/${id}`);
    if (!res.success) { showToast('Failed to load filter.', 'error'); return; }
    const f = res.data;
    const p = new URLSearchParams();
    if (f.dateFrom) p.set('dateFrom', new Date(f.dateFrom).toISOString().slice(0, 10));
    if (f.dateTo) p.set('dateTo', new Date(f.dateTo).toISOString().slice(0, 10));
    if (f.transactionTypes?.length === 1) p.set('type', f.transactionTypes[0]);
    if (f.categoryIds?.length === 1) p.set('categoryId', f.categoryIds[0]);
    if (f.accountIds?.length === 1) p.set('accountId', f.accountIds[0]);
    if (f.tags?.length) p.set('tag', f.tags[0]);
    if (f.paymentMethods?.length) p.set('paymentMethod', f.paymentMethods[0]);
    if (f.statuses?.length) p.set('status', f.statuses[0]);
    if (f.currencies?.length) p.set('currency', f.currencies[0]);
    if (f.minAmount != null) p.set('minAmount', f.minAmount);
    if (f.maxAmount != null) p.set('maxAmount', f.maxAmount);
    if (f.queryNote) p.set('search', f.queryNote);
    if (f.excludeFuture === false) p.set('excludeFuture', 'false');
    location.hash = '#/records?' + p.toString();
  } catch (e) { if (e.message !== 'Session expired') showToast('Error applying filter.', 'error'); }
};

function buildFilterForm(existing) {
  const f = existing || {};
  const timelineOpts = [
    {value:'ALL_TIME',label:'All Time'},{value:'THIS_MONTH',label:'This Month'},
    {value:'LAST_MONTH',label:'Last Month'},{value:'THIS_YEAR',label:'This Year'},
    {value:'LAST_YEAR',label:'Last Year'},{value:'CUSTOM',label:'Custom Range'},
  ];
  const selectedTimeline = f.timelineType || 'ALL_TIME';
  const showCustom = selectedTimeline === 'CUSTOM';
  const fromVal = f.dateFrom ? new Date(f.dateFrom).toISOString().slice(0,10) : '';
  const toVal   = f.dateTo   ? new Date(f.dateTo).toISOString().slice(0,10) : '';

  const catOptions = (cachedCategories||[]).map(c =>
    `<option value="${c.id}" ${(f.categoryIds||[]).includes(c.id)?'selected':''}>${esc(c.name)}</option>`
  ).join('');
  const accOptions = (cachedAccounts||[]).map(a =>
    `<option value="${esc(a.id)}" ${(f.accountIds||[]).includes(a.id)?'selected':''}>${esc(a.name)}</option>`
  ).join('');
  const tagOptions = (cachedTags||[]).map(t =>
    `<option value="${esc(t)}" ${(f.tags||[]).includes(t)?'selected':''}>${esc(t)}</option>`
  ).join('');

  return `
    ${formField('Filter Name', `<input type="text" id="fl-name" value="${esc(f.name||'')}" placeholder="My filter">`)}
    <div class="form-field">
      <label>Timeline</label>
      ${buildSelect('fl-timeline', timelineOpts, selectedTimeline, '', false, `document.getElementById('fl-custom-range').style.display=this.value==='CUSTOM'?'block':'none'`)}
    </div>
    <div id="fl-custom-range" style="display:${showCustom?'block':'none'}">
      <div class="form-field"><label>Date Range</label>
        <div class="date-range-wrap">
          <input type="date" id="fl-from" value="${esc(fromVal)}">
          <span class="date-range-sep">→</span>
          <input type="date" id="fl-to" value="${esc(toVal)}">
        </div>
      </div>
    </div>
    <div class="form-field"><label>Transaction Types</label>
      <div class="chip-group" id="fl-types">
        ${['EXPENSE','INCOME','TRANSFER'].map(t=>`
          <label class="chip-check ${(f.transactionTypes||[]).includes(t)?'active':''}">
            <input type="checkbox" name="fl-type" value="${t}" ${(f.transactionTypes||[]).includes(t)?'checked':''}
              onchange="this.closest('label').classList.toggle('active',this.checked)">
            <span>${t.charAt(0)+t.slice(1).toLowerCase()}</span>
          </label>`).join('')}
      </div>
    </div>
    ${catOptions ? `<div class="form-field"><label>Categories</label><select id="fl-categories" multiple style="height:80px;width:100%">${catOptions}</select></div>` : ''}
    ${accOptions ? `<div class="form-field"><label>Accounts</label><select id="fl-accounts" multiple style="height:80px;width:100%">${accOptions}</select></div>` : ''}
    ${tagOptions ? `<div class="form-field"><label>Tags</label><select id="fl-tags" multiple style="height:80px;width:100%">${tagOptions}</select></div>` : ''}
    <div class="form-field"><label>Payment Methods</label>
      <div class="chip-group">
        ${['CASH','CARD','BANK_TRANSFER','OTHER'].map(pm=>`
          <label class="chip-check ${(f.paymentMethods||[]).includes(pm)?'active':''}">
            <input type="checkbox" name="fl-pm" value="${pm}" ${(f.paymentMethods||[]).includes(pm)?'checked':''}
              onchange="this.closest('label').classList.toggle('active',this.checked)">
            <span>${pm.replace(/_/g,' ')}</span>
          </label>`).join('')}
      </div>
    </div>
    <div class="form-field"><label>Statuses</label>
      <div class="chip-group">
        ${['CLEARED','PENDING','VOID'].map(s=>`
          <label class="chip-check ${(f.statuses||[]).includes(s)?'active':''}">
            <input type="checkbox" name="fl-status" value="${s}" ${(f.statuses||[]).includes(s)?'checked':''}
              onchange="this.closest('label').classList.toggle('active',this.checked)">
            <span>${s.charAt(0)+s.slice(1).toLowerCase()}</span>
          </label>`).join('')}
      </div>
    </div>
    <div style="display:flex;gap:12px">
      ${formField('Min Amount', `<input type="number" id="fl-min" value="${f.minAmount!=null?f.minAmount:''}" placeholder="0" step="0.01" min="0">`)}
      ${formField('Max Amount', `<input type="number" id="fl-max" value="${f.maxAmount!=null?f.maxAmount:''}" placeholder="Any" step="0.01" min="0">`)}
    </div>
    <div class="form-field"><label>Note contains</label>
      <div style="display:flex;gap:8px;align-items:center">
        <input type="text" id="fl-note" value="${esc(f.queryNote||'')}" placeholder="Search in notes" style="flex:1">
        <label style="display:flex;align-items:center;gap:4px;font-size:0.85rem;white-space:nowrap">
          <input type="checkbox" id="fl-note-regex" ${f.isNoteRegex?'checked':''}> Regex
        </label>
      </div>
    </div>
    <div class="form-field"><label>Payee contains</label>
      <div style="display:flex;gap:8px;align-items:center">
        <input type="text" id="fl-payee" value="${esc(f.queryPayee||'')}" placeholder="Search by payee" style="flex:1">
        <label style="display:flex;align-items:center;gap:4px;font-size:0.85rem;white-space:nowrap">
          <input type="checkbox" id="fl-payee-regex" ${f.isPayeeRegex?'checked':''}> Regex
        </label>
      </div>
    </div>
    <div class="form-field"><label>Warranty</label>
      <div class="chip-group">
        <label class="chip-check ${f.hasWarranty===null||f.hasWarranty===undefined?'active':''}">
          <input type="radio" name="fl-warranty" value="" ${f.hasWarranty==null?'checked':''}
            onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')">
          <span>Any</span></label>
        <label class="chip-check ${f.hasWarranty===true?'active':''}">
          <input type="radio" name="fl-warranty" value="true" ${f.hasWarranty===true?'checked':''}
            onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')">
          <span>Has warranty</span></label>
        <label class="chip-check ${f.hasWarranty===false?'active':''}">
          <input type="radio" name="fl-warranty" value="false" ${f.hasWarranty===false?'checked':''}
            onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')">
          <span>No warranty</span></label>
      </div>
    </div>
    <div class="form-field" style="margin-bottom:0">
      <label style="display:flex;align-items:center;gap:8px;cursor:pointer">
        <input type="checkbox" id="fl-exclude-future" ${f.excludeFuture===false?'':'checked'}>
        <span>Exclude future-dated transactions</span>
      </label>
    </div>`;
}

function collectFilterPayload() {
  const name = document.getElementById('fl-name')?.value.trim();
  if (!name) { showFieldError('fl-name', 'Name is required.'); return null; }

  const timelineType = document.getElementById('fl-timeline')?.value || 'ALL_TIME';
  const fromStr = document.getElementById('fl-from')?.value;
  const toStr   = document.getElementById('fl-to')?.value;
  const dateFrom = (timelineType === 'CUSTOM' && fromStr) ? new Date(fromStr).getTime() : null;
  const dateTo   = (timelineType === 'CUSTOM' && toStr)   ? new Date(toStr).getTime()   : null;

  const transactionTypes = [...document.querySelectorAll('input[name="fl-type"]:checked')].map(e => e.value);
  const paymentMethods   = [...document.querySelectorAll('input[name="fl-pm"]:checked')].map(e => e.value);
  const statuses         = [...document.querySelectorAll('input[name="fl-status"]:checked')].map(e => e.value);

  const catSel = document.getElementById('fl-categories');
  const categoryIds = catSel ? [...catSel.selectedOptions].map(o => Number(o.value)) : [];
  const accSel = document.getElementById('fl-accounts');
  const accountIds  = accSel ? [...accSel.selectedOptions].map(o => o.value) : [];
  const tagSel = document.getElementById('fl-tags');
  const tags        = tagSel ? [...tagSel.selectedOptions].map(o => o.value) : [];

  const minVal = document.getElementById('fl-min')?.value;
  const maxVal = document.getElementById('fl-max')?.value;
  const minAmount = minVal !== '' && minVal != null ? parseFloat(minVal) : null;
  const maxAmount = maxVal !== '' && maxVal != null ? parseFloat(maxVal) : null;

  const queryNote    = document.getElementById('fl-note')?.value.trim() || null;
  const isNoteRegex  = document.getElementById('fl-note-regex')?.checked || false;
  const queryPayee   = document.getElementById('fl-payee')?.value.trim() || null;
  const isPayeeRegex = document.getElementById('fl-payee-regex')?.checked || false;

  const warrantyVal = document.querySelector('input[name="fl-warranty"]:checked')?.value;
  const hasWarranty = warrantyVal === 'true' ? true : warrantyVal === 'false' ? false : null;

  const excludeFuture = document.getElementById('fl-exclude-future')?.checked !== false;

  return {
    name, timelineType, dateFrom, dateTo,
    transactionTypes, paymentMethods, statuses,
    categoryIds, accountIds, tags,
    minAmount, maxAmount,
    queryNote, isNoteRegex, queryPayee, isPayeeRegex,
    hasWarranty, excludeFuture,
  };
}

window.openCreateFilter = function () {
  const body = buildFilterForm(null);
  openModal('New Saved Filter', body, async () => {
    clearFieldErrors(modalBody);
    const payload = collectFilterPayload();
    if (!payload) return;
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('POST', '/api/filters', payload);
      if (res.success) {
        closeModal();
        renderFilters();
        showToast('Filter saved!');
      } else {
        showToast('Failed to save filter: ' + (res.error || ''), 'error');
      }
    });
  }, 'Save Filter');
  modalConfBtn.className = 'btn btn-primary';
};

window.openEditFilter = async function (id) {
  try {
    const res = await apiFetch(`/api/filters/${id}`);
    if (!res.success) { showToast('Failed to load filter.', 'error'); return; }
    const f = res.data;
    const body = buildFilterForm(f);
    openModal(`Edit Filter — ${esc(f.name)}`, body, async () => {
      clearFieldErrors(modalBody);
      const payload = collectFilterPayload();
      if (!payload) return;
      await withLoading(modalConfBtn, async () => {
        const upd = await apiWrite('PUT', `/api/filters/${id}`, payload);
        if (upd.success) {
          closeModal();
          renderFilters();
          showToast('Filter updated!');
        } else {
          showToast('Failed to update filter: ' + (upd.error || ''), 'error');
        }
      });
    }, 'Save Changes');
    modalConfBtn.className = 'btn btn-primary';
  } catch (e) { if (e.message !== 'Session expired') showToast('Error loading filter.', 'error'); }
};

window.deleteFilter = function (id, name) {
  confirmDelete(`Delete filter "${name}"?`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/filters/${id}`);
      closeModal();
      if (res.success) { renderFilters(); showToast('Filter deleted.'); }
      else { showToast('Failed to delete filter.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting filter.', 'error'); }
  });
};

/* PAGE: Saved Filters */

async function renderFilters(searchQuery) {
  showSpinner();
  try {
    const json = await apiFetch('/api/filters');
    if (!json.success) { showError('Failed to load filters.'); return; }
    const allFilters = json.data || [];
    const q = (searchQuery || '').toLowerCase().trim();
    const filters = q ? allFilters.filter(f => (f.name || '').toLowerCase().includes(q)) : allFilters;

    let html = `<div class="page-header">
      <h2 class="section-title">Saved Filters</h2>
      <button class="btn btn-primary btn-sm" onclick="openCreateFilter()">+ New Filter</button>
    </div>
    <div class="search-bar-wrap" style="margin-bottom:16px">
      <input type="text" class="search-bar" id="filter-search" placeholder="Search saved filters…"
        value="${esc(q)}" oninput="renderFilters(this.value)">
    </div>`;

    if (!allFilters.length) {
      html += emptyState('', 'No saved filters yet.', '+ New Filter', 'openCreateFilter()');
      setContent(html);
      return;
    }

    if (!filters.length) {
      html += `<div style="text-align:center;padding:32px;color:var(--text-muted)">No filters match "<strong>${esc(q)}</strong>"</div>`;
      setContent(html);
      document.getElementById('filter-search')?.focus();
      return;
    }

    html += `<div class="filter-cards-grid">`;
    filters.forEach(f => {
      const chips   = buildFilterChips(f);
      const count   = countFilterCriteria(f);
      html += `<div class="filter-card">
        <div class="filter-card-header">
          <span class="filter-card-icon">⊟</span>
          <span class="filter-card-name">${esc(f.name)}</span>
          ${count ? `<span class="filter-card-count">${count} criteri${count === 1 ? 'on' : 'a'}</span>` : ''}
        </div>
        <div class="filter-card-chips">
          ${chips || '<span style="font-size:0.8rem;color:var(--text-muted);font-style:italic">No criteria set — matches all records</span>'}
        </div>
        <div class="filter-card-actions">
          <button class="btn btn-primary btn-sm filter-apply-btn" onclick="applyFilter(${f.id})">▶ Apply to Records</button>
          <button class="btn-icon" title="Edit filter" onclick="openEditFilter(${f.id})">Edit</button>
          <button class="btn-icon" title="Delete filter" style="color:var(--danger)" onclick="deleteFilter(${f.id},'${esc(f.name)}')">✕</button>
        </div>
      </div>`;
    });
    html += `</div>`;

    setContent(html);
    const inp = document.getElementById('filter-search');
    if (inp && q) { inp.focus(); inp.setSelectionRange(q.length, q.length); }
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading filters.'); }
}

function countFilterCriteria(f) {
  let n = 0;
  if (f.transactionTypes?.length) n++;
  if (f.categoryIds?.length) n++;
  if (f.accountIds?.length) n++;
  if (f.tags?.length) n++;
  if (f.paymentMethods?.length) n++;
  if (f.statuses?.length) n++;
  if (f.currencies?.length) n++;
  if (f.timelineType && f.timelineType !== 'ALL_TIME') n++;
  if (f.dateFrom || f.dateTo) n++;
  if (f.minAmount != null || f.maxAmount != null) n++;
  if (f.queryPayee) n++;
  if (f.queryNote) n++;
  if (f.hasWarranty != null) n++;
  return n;
}

function buildFilterChips(f) {
  const chips = [];

  // Transaction type chips — colored by type
  const typeColor = { EXPENSE: 'var(--danger)', INCOME: '#2e7d32', TRANSFER: 'var(--accent)' };
  (f.transactionTypes || []).forEach(t => {
    const col = typeColor[t] || 'var(--text-muted)';
    chips.push(`<span class="fcl-chip" style="background:${col};color:#fff">${esc(t.charAt(0) + t.slice(1).toLowerCase())}</span>`);
  });

  // Category chips — use category color when available
  (f.categoryIds || []).slice(0, 4).forEach((id, i, arr) => {
    const cat = (cachedCategories || []).find(x => x.id === id);
    const name = cat ? cat.name : `#${id}`;
    const col  = cat ? (typeof cat.colorHex === 'number' ? '#' + ((cat.colorHex >>> 0) & 0xFFFFFF).toString(16).padStart(6,'0') : cat.colorHex || '#9e9e9e') : '#9e9e9e';
    chips.push(`<span class="fcl-chip fcl-chip-cat" style="border-left:3px solid ${esc(col)}">${esc(name)}</span>`);
  });
  if ((f.categoryIds || []).length > 4) {
    chips.push(`<span class="fcl-chip fcl-chip-more">+${f.categoryIds.length - 4} more</span>`);
  }

  // Account chips
  (f.accountIds || []).slice(0, 3).forEach(id => {
    const acc = (cachedAccounts || []).find(x => x.id === id);
    const name = acc ? acc.name : id;
    const col  = acc ? (acc.colorHex || '#9e9e9e') : '#9e9e9e';
    chips.push(`<span class="fcl-chip fcl-chip-acc" style="border-left:3px solid ${esc(col)}">🏦 ${esc(name)}</span>`);
  });
  if ((f.accountIds || []).length > 3) {
    chips.push(`<span class="fcl-chip fcl-chip-more">+${f.accountIds.length - 3} more</span>`);
  }

  // Tag chips
  (f.tags || []).slice(0, 4).forEach(tag => {
    chips.push(`<span class="fcl-chip fcl-chip-tag"># ${esc(tag)}</span>`);
  });
  if ((f.tags || []).length > 4) {
    chips.push(`<span class="fcl-chip fcl-chip-more">+${f.tags.length - 4} more</span>`);
  }

  // Timeline / date chips
  if (f.timelineType && f.timelineType !== 'ALL_TIME') {
    const label = f.timelineType.replace(/_/g,' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase());
    chips.push(`<span class="fcl-chip fcl-chip-meta">📅 ${esc(label)}</span>`);
  }
  if (f.dateFrom || f.dateTo) {
    const from = f.dateFrom ? new Date(f.dateFrom).toLocaleDateString() : '…';
    const to   = f.dateTo   ? new Date(f.dateTo).toLocaleDateString()   : '…';
    chips.push(`<span class="fcl-chip fcl-chip-meta">📅 ${esc(from)} → ${esc(to)}</span>`);
  }

  // Amount range chip
  if (f.minAmount != null || f.maxAmount != null) {
    const lo = f.minAmount != null ? `≥${f.minAmount}` : '';
    const hi = f.maxAmount != null ? `≤${f.maxAmount}` : '';
    chips.push(`<span class="fcl-chip fcl-chip-meta">$ ${esc([lo,hi].filter(Boolean).join(' '))}</span>`);
  }

  // Status chips
  const statusColor = { CLEARED: '#2e7d32', UNCLEARED: '#f57f17', CANCELLED: '#757575' };
  (f.statuses || []).forEach(s => {
    const col = statusColor[s] || '#9e9e9e';
    chips.push(`<span class="fcl-chip" style="color:${col};border:1px solid ${col}">${esc(s.charAt(0)+s.slice(1).toLowerCase())}</span>`);
  });

  // Payment method chips
  const pmLabel = { CASH:'Cash', DEBIT_CARD:'Debit', CREDIT_CARD:'Credit', BANK_TRANSFER:'Bank Xfer', DIGITAL_WALLET:'Digital', OTHER:'Other' };
  (f.paymentMethods || []).forEach(pm => {
    chips.push(`<span class="fcl-chip fcl-chip-meta">${esc(pmLabel[pm] || pm)}</span>`);
  });

  // Currency chips
  (f.currencies || []).forEach(cur => {
    chips.push(`<span class="fcl-chip fcl-chip-meta">💱 ${esc(cur)}</span>`);
  });

  // Payee / note text search
  if (f.queryPayee) chips.push(`<span class="fcl-chip fcl-chip-meta">Payee: "${esc(f.queryPayee)}"</span>`);
  if (f.queryNote)  chips.push(`<span class="fcl-chip fcl-chip-meta">Note: "${esc(f.queryNote)}"</span>`);
  if (f.hasWarranty === true)  chips.push(`<span class="fcl-chip fcl-chip-meta">Has warranty</span>`);
  if (f.excludeFuture === false) chips.push(`<span class="fcl-chip fcl-chip-meta">Include future</span>`);

  return chips.join('');
}

window.applyFilter = async function (id) {
  try {
    const res = await apiFetch(`/api/filters/${id}`);
    if (!res.success) { showToast('Failed to load filter.', 'error'); return; }
    const f = res.data;
    const p = new URLSearchParams();
    if (f.dateFrom) p.set('dateFrom', new Date(f.dateFrom).toISOString().slice(0, 10));
    if (f.dateTo)   p.set('dateTo',   new Date(f.dateTo).toISOString().slice(0, 10));
    if (f.transactionTypes?.length) p.set('types',          f.transactionTypes.join(','));
    if (f.categoryIds?.length)      p.set('categoryIds',    f.categoryIds.join(','));
    if (f.accountIds?.length)       p.set('accountIds',     f.accountIds.join(','));
    if (f.tags?.length)             p.set('tagNames',       f.tags.join(','));
    if (f.paymentMethods?.length)   p.set('paymentMethods', f.paymentMethods.join(','));
    if (f.statuses?.length)         p.set('statuses',       f.statuses.join(','));
    if (f.currencies?.length)       p.set('currencies',     f.currencies.join(','));
    if (f.minAmount != null)        p.set('minAmount',      f.minAmount);
    if (f.maxAmount != null)        p.set('maxAmount',      f.maxAmount);
    if (f.queryPayee)               p.set('queryPayee',     f.queryPayee);
    if (f.queryNote)                p.set('queryNote',      f.queryNote);
    if (f.excludeFuture === false)  p.set('excludeFuture',  'false');
    p.set('page', '0');
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

  const catItems = (cachedCategories||[]).map(c => ({value: String(c.id), label: c.name}));
  const accItems = (cachedAccounts||[]).map(a => ({value: a.id, label: a.name}));
  const tagItems = (cachedTags||[]).map(t => ({value: t, label: t}));
  const selCatIds = (f.categoryIds||[]).map(String);
  const selAccIds = (f.accountIds||[]).map(String);
  const selTags   = (f.tags||[]).map(String);

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
    ${catItems.length ? `<div class="form-field"><label>Categories</label>${buildMultiSelect('fl-categories', catItems, selCatIds, 'Search categories…')}</div>` : ''}
    ${accItems.length ? `<div class="form-field"><label>Accounts</label>${buildMultiSelect('fl-accounts', accItems, selAccIds, 'Search accounts…')}</div>` : ''}
    ${tagItems.length ? `<div class="form-field"><label>Tags</label>${buildMultiSelect('fl-tags', tagItems, selTags, 'Search tags…')}</div>` : ''}
    <div class="form-field"><label>Payment Methods</label>
      <div class="chip-group">
        ${[['CASH','Cash'],['DEBIT_CARD','Debit Card'],['CREDIT_CARD','Credit Card'],['BANK_TRANSFER','Bank Transfer'],['DIGITAL_WALLET','Digital Wallet'],['OTHER','Other']].map(([v,l])=>`
          <label class="chip-check ${(f.paymentMethods||[]).includes(v)?'active':''}">
            <input type="checkbox" name="fl-pm" value="${v}" ${(f.paymentMethods||[]).includes(v)?'checked':''}
              onchange="this.closest('label').classList.toggle('active',this.checked)">
            <span>${l}</span>
          </label>`).join('')}
      </div>
    </div>
    <div class="form-field"><label>Status</label>
      <div class="chip-group">
        ${[['CLEARED','Cleared'],['UNCLEARED','Uncleared'],['CANCELLED','Cancelled']].map(([v,l])=>`
          <label class="chip-check ${(f.statuses||[]).includes(v)?'active':''}">
            <input type="checkbox" name="fl-status" value="${v}" ${(f.statuses||[]).includes(v)?'checked':''}
              onchange="this.closest('label').classList.toggle('active',this.checked)">
            <span>${l}</span>
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

  const categoryIds = msGetSelected('fl-categories').map(Number);
  const accountIds  = msGetSelected('fl-accounts');
  const tags        = msGetSelected('fl-tags');

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

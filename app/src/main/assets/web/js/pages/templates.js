/* PAGE: Templates */

/* ── helpers ─────────────────────────────────────────────── */

function _tplTypeColor(t) {
  if (t.transactionType === 'INCOME')   return 'var(--income)';
  if (t.transactionType === 'TRANSFER') return 'var(--accent)';
  return 'var(--expense)';
}

function _tplTypeBadgeIcon(t) {
  if (t.transactionType === 'INCOME')   return `<svg viewBox="0 0 24 24" fill="currentColor" width="10" height="10"><path d="M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"/></svg>`;
  if (t.transactionType === 'TRANSFER') return `<svg viewBox="0 0 24 24" fill="currentColor" width="10" height="10"><path d="M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"/></svg>`;
  return `<svg viewBox="0 0 24 24" fill="currentColor" width="10" height="10"><path d="M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"/></svg>`;
}

function _tplCatIcon() {
  return `<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 2l-5.5 9h11L12 2zm0 3.84L13.93 9h-3.87L12 5.84zM17.5 13c-2.49 0-4.5 2.01-4.5 4.5S15.01 22 17.5 22s4.5-2.01 4.5-4.5S19.99 13 17.5 13zm0 7c-1.38 0-2.5-1.12-2.5-2.5S16.12 15 17.5 15s2.5 1.12 2.5 2.5S18.88 20 17.5 20zM3 21.5h8v-8H3v8zm2-6h4v4H5v-4z"/></svg>`;
}

function _tplListItem(t) {
  const typeColor = _tplTypeColor(t);
  const typeBadgeBg = t.transactionType === 'INCOME' ? 'var(--income)' : t.transactionType === 'TRANSFER' ? 'var(--accent)' : 'var(--expense)';
  const cat = t.categoryId ? (cachedCategories.find(c => String(c.id) === String(t.categoryId))?.name || t.categoryName || '—') : (t.categoryName || '—');
  const acc = t.accountId ? (cachedAccounts.find(a => String(a.id) === String(t.accountId))?.name || t.accountName || '') : '';
  const sign = t.transactionType === 'INCOME' ? '+' : t.transactionType === 'TRANSFER' ? '' : '−';
  const starIcon = t.isStarred
    ? `<svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" style="color:var(--accent)"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>`
    : '';

  return `<div class="tpl-item" onclick="openEditTemplate('${esc(t.id)}')">
    <div class="tpl-item-icon-wrap">
      <div class="tpl-item-icon">${_tplCatIcon()}</div>
      <div class="tpl-item-type-badge" style="background:${typeBadgeBg}">${_tplTypeBadgeIcon(t)}</div>
    </div>
    <div class="tpl-item-body">
      <div class="tpl-item-header">
        <span class="tpl-item-name">${esc(t.name)} ${starIcon}</span>
        <span class="tpl-item-amount" style="color:${typeColor}">${sign}${fmt(t.amount, t.currency)}</span>
      </div>
      <div class="tpl-item-sub">
        <span>${esc(cat)}</span>
        ${acc ? `<span class="tpl-item-dot">·</span><span>${esc(acc)}</span>` : ''}
        ${t.tags && t.tags.length ? `<span class="tpl-item-dot">·</span><span style="color:var(--text-muted)">${t.tags.slice(0,2).map(tag => esc(tag)).join(', ')}</span>` : ''}
      </div>
    </div>
    <div class="tpl-item-actions">
      <button class="tpl-menu-btn primary" onclick="event.stopPropagation();useTemplate('${esc(t.id)}')">Use</button>
      <button class="tpl-menu-btn"         onclick="event.stopPropagation();createPlannedFromTemplate('${esc(t.id)}')">→ Planned</button>
      <button class="tpl-menu-btn"         onclick="event.stopPropagation();toggleTemplateStar('${esc(t.id)}',${!t.isStarred})">${t.isStarred ? 'Unstar' : '★ Star'}</button>
      <button class="tpl-menu-btn danger"  onclick="event.stopPropagation();deleteTemplate('${esc(t.id)}','${esc(t.name)}')">Delete</button>
    </div>
  </div>`;
}

/* ── list page ───────────────────────────────────────────── */

async function renderTemplates() {
  showSpinner();
  try {
    const json = await apiFetch('/api/templates');
    if (!json.success) { showError('Failed to load templates.'); return; }
    const templates = json.data || [];

    let html = `<div class="page-header">
      <h2 class="section-title">Templates</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddTemplate()">+ New Template</button>
    </div>`;

    if (!templates.length) {
      html += emptyState('', 'No templates yet.', '+ Add Template', 'openAddTemplate()');
      setContent(html);
      return;
    }

    const starred = templates.filter(t => t.isStarred);
    const rest    = templates.filter(t => !t.isStarred);

    html += '<div class="tpl-list">';
    if (starred.length) {
      html += '<div class="tpl-section-header">★ STARRED</div>';
      starred.forEach(t => { html += _tplListItem(t); });
    }
    if (rest.length) {
      if (starred.length) html += '<div class="tpl-section-header">ALL TEMPLATES</div>';
      rest.forEach(t => { html += _tplListItem(t); });
    }
    html += '</div>';

    setContent(html);
  } catch(e) { if (e.message !== 'Session expired') showError('Error loading templates.'); }
}

/* ── actions ─────────────────────────────────────────────── */

window.toggleTemplateStar = async function (id, star) {
  try {
    const res = await apiWrite('PUT', `/api/templates/${id}`, { isStarred: star });
    if (res.success) { renderTemplates(); showToast(star ? 'Template starred.' : 'Template unstarred.'); }
    else { showToast('Failed.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error.', 'error'); }
};

window.createPlannedFromTemplate = async function (id) {
  const resp = await apiFetch(`/api/templates/${id}`);
  if (!resp.success) { showToast('Could not load template.', 'error'); return; }
  const t = resp.data;
  // Pre-fill the add planned payment modal with template data
  openAddPlanned();
  await new Promise(r => setTimeout(r, 50));
  const set = (elId, val) => { const el = document.getElementById(elId); if (el && val != null) el.value = val; };
  set('pp-name', t.name);
  set('pp-amount', t.amount);
  set('pp-currency', t.currency);
  set('pp-type', t.transactionType || 'EXPENSE');
  if (t.accountId) set('pp-account', t.accountId);
  if (t.categoryId) set('pp-category', String(t.categoryId));
  if (t.payee) set('pp-payee', t.payee);
  if (t.note) set('pp-note', t.note);
};

window.useTemplate = async function (id) {
  const resp = await apiFetch(`/api/templates/${id}`);
  if (!resp.success) { showToast('Could not load template.', 'error'); return; }
  const t = resp.data;
  location.hash = '#/records';
  await new Promise(r => setTimeout(r, 80));
  openAddTransaction({
    title: t.name,
    amount: t.amount,
    currency: t.currency,
    type: t.transactionType,
    accountId: t.accountId,
    categoryId: t.categoryId,
    payee: t.payee,
    paymentMethod: t.paymentMethod,
    tags: t.tags,
    note: t.note,
    status: t.transactionStatus,
  });
};

/* ── form body ───────────────────────────────────────────── */

function templateFormBody(t) {
  return `
    ${formRow(formField('Template Name', `<input type="text" id="tpl-name" placeholder="e.g. Monthly Rent" value="${esc(t?.name||'')}">`),
              formField('Amount', `<input type="number" id="tpl-amount" placeholder="0.00" min="0" step="0.01" value="${esc(t?.amount||'')}">`))}
    ${formRow(formField('Currency', `<input type="text" id="tpl-currency" value="${esc(t?.currency||cachedSettings.baseCurrency||'USD')}" maxlength="5">`),
              formField('Type', buildSelect('tpl-type',[{value:'EXPENSE',label:'Expense'},{value:'INCOME',label:'Income'},{value:'TRANSFER',label:'Transfer'}],(t?.transactionType||'EXPENSE'),'— Select —',false,"document.getElementById('tpl-to-account-row').style.display=this.value==='TRANSFER'?'':'none'")))}
    <div id="tpl-to-account-row" style="${(t?.transactionType||'EXPENSE')==='TRANSFER'?'':'display:none'}">
      ${formField('To Account', buildSelect('tpl-to-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),t?.targetAccountId||'','— Select destination —',true))}
    </div>
    ${formField('Account', buildSelect('tpl-account',[{value:'',label:'— None —'}].concat(cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`}))),t?.accountId||'','— None —',true))}
    ${formField('Category', buildSelect('tpl-category',[{value:'0',label:'— None —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),t?.categoryId?String(t.categoryId):'0','— None —',true))}
    ${formRow(
      formField('Payee', `<input type="text" id="tpl-payee" placeholder="Optional payee" value="${esc(t?.payee||'')}">`),
      formField('Location', `<input type="text" id="tpl-location" placeholder="Optional location" value="${esc(t?.location||'')}">`)
    )}
    ${formField('Payment Method', buildSelect('tpl-method',[{value:'CASH',label:'Cash'},{value:'CARD',label:'Card'},{value:'BANK_TRANSFER',label:'Bank Transfer'},{value:'DIGITAL_WALLET',label:'Digital Wallet'},{value:'OTHER',label:'Other'}],(t?.paymentMethod||'CASH'),'— Select —',false))}
    ${formField('Status', `<div class="chip-group">
      ${['CLEARED','UNCLEARED','RECONCILE'].map(s=>`<label class="chip-check ${(t?.transactionStatus||'CLEARED')===s?'active':''}"><input type="radio" name="tpl-status" value="${s}" ${(t?.transactionStatus||'CLEARED')===s?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>${prettyEnum(s)}</span></label>`).join('')}
    </div>`)}
    ${formField('Tags', `<div id="tpl-tags-container"></div>`)}
    ${formField('Note', `<textarea id="tpl-note" placeholder="Optional note…">${esc(t?.note||'')}</textarea>`)}
    <small style="color:var(--text-muted);font-size:0.75rem">Templates are quick-fill presets for manual transactions. For automatic recurring payments, use Planned Payments.</small>`;
}

window.openAddTemplate = function () {
  const body = templateFormBody(null);
  openModal('New Template', body, submitAddTemplate, 'Save Template');
  modalConfBtn.className = 'btn btn-primary';
  initTagChipInput('tpl-tags', cachedTags, []);
};

window.submitAddTemplate = async function () {
  clearFieldErrors(modalBody);
  const get = id => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  const name = get('tpl-name');
  const txType = get('tpl-type');
  if (!name) { showFieldError('tpl-name', 'Template name is required.'); return; }
  const payload = {
    name,
    amount:          parseFloat(get('tpl-amount')) || 0,
    currency:        get('tpl-currency') || cachedSettings.baseCurrency,
    transactionType: txType,
    accountId:       get('tpl-account') || null,
    categoryId:      parseInt(get('tpl-category')) || 0,
    payee:           get('tpl-payee') || null,
    location:        get('tpl-location') || null,
    paymentMethod:   get('tpl-method') || null,
    tags:            get('tpl-tags') ? get('tpl-tags').split(',').map(t=>t.trim()).filter(Boolean) : [],
    note:            get('tpl-note') || null,
    transactionStatus: document.querySelector('input[name="tpl-status"]:checked')?.value || 'CLEARED',
  };
  if (txType === 'TRANSFER') payload.targetAccountId = get('tpl-to-account') || null;
  await withLoading(modalConfBtn, async () => {
    const res = await apiWrite('POST', '/api/templates', payload);
    if (res.success) { closeModal(); renderTemplates(); showToast('Template saved!'); }
    else { showToast('Failed: ' + (res.error||''), 'error'); }
  });
};

window.openEditTemplate = async function (id) {
  const resp = await apiFetch(`/api/templates/${id}`);
  if (!resp.success) { showToast('Could not load template.', 'error'); return; }
  const t = resp.data;
  const body = templateFormBody(t);
  openModal('Edit Template', body, async () => {
    const get = i => { const el = document.getElementById(i); return el ? el.value.trim() : ''; };
    const txType = get('tpl-type');
    const payload = {
      name:            get('tpl-name') || undefined,
      amount:          parseFloat(get('tpl-amount')) || undefined,
      currency:        get('tpl-currency') || undefined,
      transactionType: txType || undefined,
      accountId:       get('tpl-account') || undefined,
      categoryId:      parseInt(get('tpl-category')) || undefined,
      payee:           get('tpl-payee') || undefined,
      location:        get('tpl-location') || undefined,
      paymentMethod:   get('tpl-method') || undefined,
      tags:            get('tpl-tags') ? get('tpl-tags').split(',').map(t=>t.trim()).filter(Boolean) : [],
      note:            get('tpl-note') || undefined,
      transactionStatus: document.querySelector('input[name="tpl-status"]:checked')?.value || 'CLEARED',
    };
    if (txType === 'TRANSFER') payload.targetAccountId = get('tpl-to-account') || undefined;
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/templates/${id}`, payload);
      if (res.success) { closeModal(); renderTemplates(); showToast('Template updated!'); }
      else { showToast('Failed: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
  initTagChipInput('tpl-tags', cachedTags, t.tags || []);
  if (t.accountId)       { const el = document.getElementById('tpl-account');     if (el) el.value = t.accountId; }
  if (t.categoryId)      { const el = document.getElementById('tpl-category');    if (el) el.value = String(t.categoryId); }
  if (t.targetAccountId) { const el = document.getElementById('tpl-to-account'); if (el) el.value = t.targetAccountId; }
};

window.deleteTemplate = function (id, name) {
  confirmDelete(`Delete template "${name}"?`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/templates/${id}`);
      closeModal();
      if (res.success) { renderTemplates(); showToast('Template deleted.'); }
      else { showToast('Failed to delete.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting template.', 'error'); }
  });
};

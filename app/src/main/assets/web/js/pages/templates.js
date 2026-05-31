/* PAGE: Templates */

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
      html += emptyState('📄', 'No templates yet.', '+ Add Template', 'openAddTemplate()');
    } else {
      html += `<div class="card"><div class="table-wrap"><table>
        <thead><tr><th>Name</th><th>Type</th><th>Amount</th><th>Category</th><th>Account</th><th></th></tr></thead><tbody>`;
      templates.forEach(t => {
        const typeCls = t.transactionType==='INCOME'?'badge-income':t.transactionType==='EXPENSE'?'badge-expense':'badge-transfer';
        const amtCls  = t.transactionType==='INCOME'?'amount-income':t.transactionType==='EXPENSE'?'amount-expense':'';
        const sign    = t.transactionType==='EXPENSE'?'−':t.transactionType==='INCOME'?'+':'';
        const cat     = cachedCategories.find(c=>c.id===t.categoryId);
        const acc     = cachedAccounts.find(a=>a.id===t.accountId);
        html += `<tr>
          <td style="font-weight:600">${esc(t.name)}</td>
          <td><span class="badge ${typeCls}">${esc(t.transactionType)}</span></td>
          <td class="${amtCls}">${sign}${fmtRaw(t.amount||0)} ${esc(t.currency||'')}</td>
          <td>${cat?esc(cat.name):'<span style="color:var(--text-muted)">—</span>'}</td>
          <td>${acc?esc(acc.name):'<span style="color:var(--text-muted)">—</span>'}</td>
          <td style="white-space:nowrap">
            <button class="btn-icon" title="Use template" onclick="useTemplate(${t.id})">➕</button>
            <button class="btn-icon" title="Edit" onclick="openEditTemplate(${t.id})">✏️</button>
            <button class="btn-icon" title="Delete" onclick="deleteTemplate(${t.id},'${esc(t.name)}')">🗑️</button>
          </td>
        </tr>`;
      });
      html += '</tbody></table></div></div>';
    }
    setContent(html);
  } catch(e) { if (e.message !== 'Session expired') showError('Error loading templates.'); }
}

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
  if (txType === 'TRANSFER') {
    payload.targetAccountId = get('tpl-to-account') || null;
  }
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
      paymentMethod:    get('tpl-method') || undefined,
      tags:             get('tpl-tags') ? get('tpl-tags').split(',').map(t=>t.trim()).filter(Boolean) : [],
      note:             get('tpl-note') || undefined,
      transactionStatus: document.querySelector('input[name="tpl-status"]:checked')?.value || 'CLEARED',
    };
    if (txType === 'TRANSFER') {
      payload.targetAccountId = get('tpl-to-account') || undefined;
    }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/templates/${id}`, payload);
      if (res.success) { closeModal(); renderTemplates(); showToast('Template updated!'); }
      else { showToast('Failed: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
  initTagChipInput('tpl-tags', cachedTags, t.tags || []);
  if (t.accountId) { const el = document.getElementById('tpl-account'); if (el) el.value = t.accountId; }
  if (t.categoryId) { const el = document.getElementById('tpl-category'); if (el) el.value = t.categoryId; }
  if (t.targetAccountId) { const el = document.getElementById('tpl-to-account'); if (el) el.value = t.targetAccountId; }
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

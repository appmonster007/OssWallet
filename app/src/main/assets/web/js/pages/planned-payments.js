/* PAGE: Planned Payments */

async function renderPlannedPayments() {
  showSpinner();
  try {
    const [allRes, upcomingRes, overdueRes] = await Promise.all([
      apiFetch('/api/planned-payments'),
      apiFetch('/api/planned-payments/upcoming?days=30'),
      apiFetch('/api/planned-payments/overdue'),
    ]);
    const allPayments = (allRes.success  ? (allRes.data  || []) : []);
    const upcoming    = (upcomingRes.success ? (upcomingRes.data || []) : []);
    const overdue     = (overdueRes.success  ? (overdueRes.data  || []) : []);

    let html = `<div class="page-header"><h2 class="section-title">Planned Payments</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddPlanned()">+ Add Planned Payment</button>
    </div>`;

    if (overdue.length) {
      html += `<div class="card" style="border-color:var(--danger)">
        <div class="overdue-header">Overdue (${overdue.length})</div>
        <div class="table-wrap"><table>
          <thead><tr><th>Name</th><th>Amount</th><th>Due Date</th><th>Account</th><th>Actions</th></tr></thead><tbody>`;
      overdue.forEach(p => {
        html += `<tr>
          <td>${esc(p.name)}<br><small style="color:var(--text-muted)">${esc(p.recurrenceDescription||'')}</small></td>
          <td>${fmt(p.amount||0, p.currency)}</td>
          <td style="white-space:nowrap;color:var(--danger)">${fmtDate(p.nextDueDate)}</td>
          <td>${esc(p.accountName||'')}</td>
          <td style="white-space:nowrap">
            <button class="btn btn-sm btn-primary" onclick="confirmPlanned('${esc(p.id)}')">Confirm</button>
            <button class="btn btn-sm btn-ghost" onclick="skipPlanned('${esc(p.id)}')">Skip</button>
          </td>
        </tr>`;
      });
      html += '</tbody></table></div></div>';
    }

    if (upcoming.length) {
      html += `<div class="card" style="border-color:var(--warning)">
        <div class="upcoming-header">Upcoming (next 30 days)</div>
        <div class="table-wrap"><table>
          <thead><tr><th>Name</th><th>Amount</th><th>Due Date</th><th>Recurrence</th><th>Actions</th></tr></thead><tbody>`;
      upcoming.forEach(p => {
        html += `<tr>
          <td>${esc(p.name)}</td>
          <td>${fmt(p.amount||0, p.currency)}</td>
          <td style="white-space:nowrap">${fmtDate(p.nextDueDate)}</td>
          <td><small style="color:var(--text-muted)">${esc(p.recurrenceDescription||'—')}</small></td>
          <td style="white-space:nowrap">
            <button class="btn btn-sm btn-primary" onclick="confirmPlanned('${esc(p.id)}')">Confirm</button>
            <button class="btn btn-sm btn-ghost" onclick="skipPlanned('${esc(p.id)}')">Skip</button>
          </td>
        </tr>`;
      });
      html += '</tbody></table></div></div>';
    }

    html += `<div class="section-sub">All Planned Payments</div>`;
    if (!allPayments.length) {
      html += emptyState('', 'No planned payments yet.', '+ Add Planned Payment', 'openAddPlanned()');
    } else {
      html += `<div class="card"><div class="table-wrap"><table>
        <thead><tr><th>Name</th><th>Amount</th><th>Next Due</th><th>Type</th><th>Status</th><th>Auto</th><th></th></tr></thead><tbody>`;
      allPayments.forEach(p => {
        const txBadge = p.transactionType === 'INCOME' ? 'badge-income' : 'badge-expense';
        html += `<tr>
          <td>${esc(p.name)}<br><small style="color:var(--text-muted)">${esc(p.recurrenceDescription||'')}</small></td>
          <td>${fmt(p.amount||0, p.currency)}</td>
          <td style="white-space:nowrap${p.isOverdue ? ';color:var(--danger)' : ''}">${fmtDate(p.nextDueDate)}</td>
          <td><span class="badge ${txBadge}">${esc(p.transactionType||'')}</span></td>
          <td><span class="badge ${p.isOverdue ? 'badge-overdue' : 'badge-active'}">${esc(p.status||'')}</span></td>
          <td>${p.autoConfirm ? '✅' : '—'}</td>
          <td style="white-space:nowrap">
            <button class="btn-icon" title="Edit" onclick="openEditPlanned('${esc(p.id)}')">Edit</button>
            <button class="btn-icon" title="Delete" onclick="deletePlanned('${esc(p.id)}','${esc(p.name)}')">Del</button>
          </td>
        </tr>`;
      });
      html += '</tbody></table></div></div>';
    }

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading planned payments.'); }
}

window.confirmPlanned = async function (id) {
  try {
    const res = await apiWrite('POST', `/api/planned-payments/${encodeURIComponent(id)}/confirm`);
    if (res.success) { showToast('Payment confirmed!'); refreshCurrentView(); }
    else { showToast('Failed to confirm payment.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error confirming payment.', 'error'); }
};

window.skipPlanned = async function (id) {
  try {
    const res = await apiWrite('POST', `/api/planned-payments/${encodeURIComponent(id)}/skip`);
    if (res.success) { showToast('Payment skipped.'); refreshCurrentView(); }
    else { showToast('Failed to skip payment.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error skipping payment.', 'error'); }
};

window.openAddPlanned = function () {
  const body = `
    ${formField('Payment Name', '<input type="text" id="pp-name" placeholder="e.g. Netflix">')}
    ${formRow(
      formField('Amount', `<div class="input-group"><span class="input-group-addon" id="pp-amount-pfx">${esc(cachedSettings.baseCurrency||'USD')}</span><input type="number" id="pp-amount" placeholder="0.00" min="0" step="0.01"></div>`),
      formField('Currency', `<input type="text" id="pp-currency" value="${esc(cachedSettings.baseCurrency||'USD')}" maxlength="5" oninput="const p=document.getElementById('pp-amount-pfx');if(p)p.textContent=this.value.toUpperCase()||'—'">`)
    )}
    ${formRow(
      formField('Type', buildSelect('pp-type',[{value:'EXPENSE',label:'Expense'},{value:'INCOME',label:'Income'},{value:'TRANSFER',label:'Transfer'}],'EXPENSE','— Select —',false,"document.getElementById('pp-to-account-row').style.display=this.value==='TRANSFER'?'':'none'")),
      formField('Account', buildSelect('pp-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),'','— Select account —',true))
    )}
    <div id="pp-to-account-row" style="display:none">
      ${formField('To Account', buildSelect('pp-to-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),'','— Select destination —',true))}
    </div>
    ${formField('Category', buildSelect('pp-category',[{value:'',label:'— None —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),'','— None —',true))}
    ${formRow(
      formField('Recurrence', buildSelect('pp-recur',[{value:'ONE_TIME',label:'One Time'},{value:'DAILY',label:'Daily'},{value:'WEEKLY',label:'Weekly'},{value:'MONTHLY',label:'Monthly'},{value:'YEARLY',label:'Yearly'}],'MONTHLY','— Select —',false,'document.getElementById(\'pp-interval-row\').style.display=this.value===\'ONE_TIME\'?\'none\':\'\'') ),
      formField('Interval', '<input type="number" id="pp-interval" value="1" min="1" step="1">')
    )}
    <div id="pp-interval-row" style="display:none;margin-top:-8px"><small style="color:var(--text-muted);font-size:0.75rem">Number of periods between occurrences</small></div>
    ${formRow(formField('Start Date', '<input type="date" id="pp-start">'),
              formField('Payment Method', buildSelect('pp-method',[{value:'',label:'— None —'},{value:'CASH',label:'Cash'},{value:'CARD',label:'Card'},{value:'BANK_TRANSFER',label:'Bank Transfer'},{value:'DIGITAL_WALLET',label:'Digital Wallet'},{value:'OTHER',label:'Other'}],'','— None —',false))
    )}
    ${formRow(
      formField('Payee', `<input type="text" id="pp-payee" placeholder="Optional payee">`),
      formField('Location', `<input type="text" id="pp-location" placeholder="Optional location">`)
    )}
    ${formField('Tags', `<div id="pp-tags-container"></div>`)}
    ${formField('Note (optional)', '<input type="text" id="pp-note" placeholder="Optional note…">')}
    <div class="form-field"><label>Auto-Confirm</label>
      <div class="toggle-wrap"><input type="checkbox" id="pp-auto"><span style="font-size:0.88rem">Automatically confirm when due</span></div>
    </div>`;
  openModal('Add Planned Payment', body, submitAddPlanned, 'Save Payment');
  modalConfBtn.className = 'btn btn-primary';
  initTagChipInput('pp-tags', cachedTags, []);
  const startEl = document.getElementById('pp-start');
  if (startEl) startEl.value = new Date().toISOString().slice(0,10);
};

window.submitAddPlanned = async function () {
  const get = id => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  const name    = get('pp-name');
  const amount  = parseFloat(get('pp-amount') || '0');
  const currency= get('pp-currency');
  const type    = get('pp-type');
  const account = get('pp-account');
  const cat     = get('pp-category');
  const recur   = get('pp-recur');
  const interval= parseInt(get('pp-interval') || '1', 10);
  const start   = get('pp-start');
  const method  = get('pp-method');
  const note    = get('pp-note');
  const auto    = document.getElementById('pp-auto')?.checked || false;
  const payee   = get('pp-payee');
  const location= get('pp-location');
  const tags    = get('pp-tags') ? get('pp-tags').split(',').map(t=>t.trim()).filter(Boolean) : [];

  clearFieldErrors(modalBody);
  if (!name)                      { showFieldError('pp-name', 'Name is required.'); return; }
  if (isNaN(amount) || amount <= 0) { showFieldError('pp-amount', 'Enter a valid amount.'); return; }
  if (!account) { showFieldError('pp-account', 'Account is required.'); return; }
  await withLoading(modalConfBtn, async () => {
    const body = {
      name, amount, currency, transactionType: type,
      accountId: account,
      categoryId: cat || undefined,
      recurrenceType: recur,
      recurrenceInterval: recur === 'ONE_TIME' ? 1 : interval,
      startDate: start || undefined,
      autoConfirm: auto,
      paymentMethod: method || undefined,
      note: note || undefined,
      payee: payee || undefined,
      location: location || undefined,
      tags
    };
    if (type === 'TRANSFER') {
      body.targetAccountId = get('pp-to-account') || undefined;
    }
    const res = await apiWrite('POST', '/api/planned-payments', body);
    if (res.success) { closeModal(); renderPlannedPayments(); showToast('Planned payment created!'); }
    else { showToast('Failed to create: ' + (res.error || ''), 'error'); }
  });
};

window.openEditPlanned = async function (id) {
  const resp = await apiFetch(`/api/planned-payments/${encodeURIComponent(id)}`);
  if (!resp.success) { showToast('Could not load planned payment.', 'error'); return; }
  const p = resp.data;
  const body = `
    ${formField('Payment Name', `<input type="text" id="pp-name" value="${esc(p.name)}">`)}
    ${formRow(
      formField('Amount', `<div class="input-group"><span class="input-group-addon" id="pp-amount-pfx">${esc(p.currency||cachedSettings.baseCurrency||'USD')}</span><input type="number" id="pp-amount" value="${esc(p.amount||0)}" min="0" step="0.01"></div>`),
      formField('Currency', `<input type="text" id="pp-currency" value="${esc(p.currency||cachedSettings.baseCurrency||'USD')}" maxlength="5" oninput="const p=document.getElementById('pp-amount-pfx');if(p)p.textContent=this.value.toUpperCase()||'—'">`)
    )}
    ${formRow(
      formField('Type', buildSelect('pp-type',[{value:'EXPENSE',label:'Expense'},{value:'INCOME',label:'Income'},{value:'TRANSFER',label:'Transfer'}],p.transactionType||'EXPENSE','— Select —',false,"document.getElementById('pp-to-account-row').style.display=this.value==='TRANSFER'?'':'none'")),
      formField('Account', buildSelect('pp-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),p.accountId||'','— Select account —',true))
    )}
    <div id="pp-to-account-row" style="${p.transactionType==='TRANSFER'?'':'display:none'}">
      ${formField('To Account', buildSelect('pp-to-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),p.targetAccountId||'','— Select destination —',true))}
    </div>
    ${formField('Category', buildSelect('pp-category',[{value:'',label:'— None —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),p.categoryId?String(p.categoryId):'','— None —',true))}
    ${formRow(
      formField('Recurrence', buildSelect('pp-recur',[{value:'ONE_TIME',label:'One Time'},{value:'DAILY',label:'Daily'},{value:'WEEKLY',label:'Weekly'},{value:'MONTHLY',label:'Monthly'},{value:'YEARLY',label:'Yearly'}],p.recurrenceType||'MONTHLY','— Select —',false)),
      formField('Interval', `<input type="number" id="pp-interval" value="${esc(p.recurrenceInterval||1)}" min="1" step="1">`)
    )}
    ${formRow(
      formField('Payee', `<input type="text" id="pp-payee" value="${esc(p.payee||'')}" placeholder="Optional payee">`),
      formField('Location', `<input type="text" id="pp-location" value="${esc(p.location||'')}" placeholder="Optional location">`)
    )}
    ${formField('Tags', `<div id="pp-tags-container"></div>`)}
    ${formField('Note (optional)', `<input type="text" id="pp-note" value="${esc(p.note||'')}" placeholder="Optional note…">`)}
    <div class="form-field"><label>Auto-Confirm</label>
      <div class="toggle-wrap"><input type="checkbox" id="pp-auto" ${p.autoConfirm?'checked':''}><span style="font-size:0.88rem">Automatically confirm when due</span></div>
    </div>`;
  openModal(`Edit Planned Payment — ${esc(p.name)}`, body, async () => {
    const get = i => { const el = document.getElementById(i); return el ? el.value.trim() : ''; };
    clearFieldErrors(modalBody);
    const name   = get('pp-name');
    const amount = parseFloat(get('pp-amount') || '0');
    const currency = get('pp-currency');
    const type   = get('pp-type');
    const account= get('pp-account');
    const cat    = get('pp-category');
    const recur  = get('pp-recur');
    const interval = parseInt(get('pp-interval') || '1', 10);
    const auto   = document.getElementById('pp-auto')?.checked || false;
    const payee  = get('pp-payee');
    const location = get('pp-location');
    const tags   = get('pp-tags') ? get('pp-tags').split(',').map(t=>t.trim()).filter(Boolean) : [];

    if (!name)                      { showFieldError('pp-name', 'Name is required.'); return; }
    if (isNaN(amount) || amount <= 0) { showFieldError('pp-amount', 'Enter a valid amount.'); return; }
    await withLoading(modalConfBtn, async () => {
      const payload = {
        name, amount, currency, transactionType: type,
        accountId: account || undefined,
        categoryId: cat || undefined,
        recurrenceType: recur,
        recurrenceInterval: interval,
        autoConfirm: auto,
        payee: payee || undefined,
        location: location || undefined,
        tags
      };
      if (type === 'TRANSFER') {
        payload.targetAccountId = get('pp-to-account') || undefined;
      }
      const res = await apiWrite('PUT', `/api/planned-payments/${encodeURIComponent(id)}`, payload);
      if (res.success) { closeModal(); renderPlannedPayments(); showToast('Planned payment updated!'); }
      else { showToast('Failed to update: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
  initTagChipInput('pp-tags', cachedTags, p.tags || []);
  if (p.accountId) { const el = document.getElementById('pp-account'); if (el) el.value = p.accountId; }
  if (p.categoryId) { const el = document.getElementById('pp-category'); if (el) el.value = p.categoryId; }
  if (p.targetAccountId) { const el = document.getElementById('pp-to-account'); if (el) el.value = p.targetAccountId; }
};

window.deletePlanned = function (id, name) {
  confirmDelete(`Delete planned payment "${name}"?`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/planned-payments/${encodeURIComponent(id)}`);
      closeModal();
      if (res.success) { renderPlannedPayments(); showToast('Planned payment deleted.'); }
      else { showToast('Failed to delete planned payment.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting.', 'error'); }
  });
};

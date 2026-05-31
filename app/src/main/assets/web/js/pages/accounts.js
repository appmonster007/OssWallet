/* PAGE: Accounts + Account Detail */

async function renderAccounts() {
  showSpinner();
  try {
    const json = await apiFetch('/api/accounts');
    if (!json.success) { showError('Failed to load accounts.'); return; }
    cachedAccounts = json.data || [];

    let html = `<div class="page-header"><h2 class="section-title">Accounts</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddAccount()">+ Add Account</button>
    </div>`;

    if (!cachedAccounts.length) {
      html += emptyState('💳', 'No accounts yet.', '+ Add Account', 'openAddAccount()');
    } else {
      html += '<div class="card-grid">';
      cachedAccounts.forEach(acc => {
        const balClass = (acc.balance || 0) >= 0 ? 'positive' : 'negative';
        html += `<div class="stat-card">
          <div class="stat-label">${esc(acc.type || '')}</div>
          <div style="font-weight:600;margin-bottom:8px">${esc(acc.name)}</div>
          <div class="stat-value ${balClass}">${fmt(acc.balance || 0, acc.currency)}</div>
          <div style="margin-top:12px;display:flex;gap:6px;flex-wrap:wrap">
            <a class="btn btn-primary btn-sm" href="#/accounts/${esc(acc.id)}">Details</a>
            <button class="btn btn-ghost btn-sm" onclick="openEditAccount('${esc(acc.id)}')">✏️ Edit</button>
            <a class="btn btn-ghost btn-sm" href="#/records?accountId=${esc(acc.id)}&accountName=${encodeURIComponent(acc.name)}">Records</a>
            <button class="btn btn-sm" style="background:var(--danger);color:#fff" onclick="deleteAccount('${esc(acc.id)}','${esc(acc.name)}')">Delete</button>
          </div>
        </div>`;
      });
      html += '</div>';
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading accounts.'); }
}

window.openAddAccount = function () {
  const body = `
    ${formField('Account Name', '<input type="text" id="acc-name" placeholder="e.g. Main Checking">')}
    ${formRow(
      formField('Currency', `<input type="text" id="acc-currency" value="${esc(cachedSettings.baseCurrency||'USD')}" maxlength="5" oninput="const p=document.getElementById('acc-bal-pfx');if(p)p.textContent=this.value.toUpperCase()||'—'">`),
      formField('Type', buildSelect('acc-type',[{value:'CHECKING',label:'Checking'},{value:'SAVINGS',label:'Savings'},{value:'CREDIT_CARD',label:'Credit Card'},{value:'INVESTMENT',label:'Investment'},{value:'CASH',label:'Cash'},{value:'OTHER',label:'Other'}],'CHECKING','— Select —',false))
    )}
    <div id="acc-credit-limit-row" style="display:none">
      <label style="display:block;font-size:0.82rem;font-weight:600;color:var(--text-muted);margin-bottom:5px;text-transform:uppercase;letter-spacing:0.04em">Credit Limit</label>
      <div class="input-group">
        <span class="input-group-addon" id="acc-cl-pfx">$</span>
        <input type="number" id="acc-credit-limit" min="0" step="0.01" placeholder="0.00">
      </div>
    </div>
    ${formField('Initial Balance (optional)', `<div class="input-group"><span class="input-group-addon" id="acc-bal-pfx">${esc(cachedSettings.baseCurrency||'USD')}</span><input type="number" id="acc-init-bal" placeholder="0.00" min="0" step="0.01"></div>`)}
    <div class="form-field"><label>Color</label>
      <div class="color-chip-row" id="acc-color-row">
        ${['#EF5350','#FF9800','#FFEE58','#66BB6A','#26C6DA','#42A5F5','#7E57C2','#EC407A'].map(c=>`<span class="color-chip${c==='#42A5F5'?' selected':''}" style="background:${c}" data-color="${c}" onclick="selectColorChip('acc-color','acc-color-row',this)"></span>`).join('')}
        <input type="color" id="acc-color-custom" style="width:32px;height:32px;border:none;border-radius:50%;cursor:pointer;padding:0" oninput="document.getElementById('acc-color').value=this.value;document.querySelectorAll('#acc-color-row .color-chip').forEach(c=>c.classList.remove('selected'))">
      </div>
      <input type="hidden" id="acc-color" value="#42A5F5">
    </div>`;
  openModal('Add Account', body, submitAddAccount, 'Save Account');
  modalConfBtn.className = 'btn btn-primary';
  document.getElementById('acc-type')?.addEventListener('change', function() {
    const row = document.getElementById('acc-credit-limit-row');
    if (row) row.style.display = ['CREDIT_CARD','CREDIT'].includes(this.value) ? '' : 'none';
  });
};

window.openEditAccount = function (id) {
  const acc = cachedAccounts.find(a => a.id === id);
  if (!acc) { showToast('Account not found.', 'error'); return; }
  const body = `
    <div class="form-field" style="background:var(--surface2);padding:10px;border-radius:var(--radius-sm);margin-bottom:4px">
      <div style="font-size:0.78rem;color:var(--text-muted)">Current Balance</div>
      <div style="font-weight:700;font-size:1.1rem">${fmt(acc.balance||0, acc.currency)}</div>
    </div>
    ${formField('Account Name', `<input type="text" id="acc-name" value="${esc(acc.name)}">`)}
    ${formRow(
      formField('Currency (read-only)', `<input type="text" id="acc-currency-ro" value="${esc(acc.currency)}" disabled style="opacity:.6">`),
      formField('Type', buildSelect('acc-type',[{value:'CHECKING',label:'Checking'},{value:'SAVINGS',label:'Savings'},{value:'CREDIT_CARD',label:'Credit Card'},{value:'INVESTMENT',label:'Investment'},{value:'CASH',label:'Cash'},{value:'DIGITAL_WALLET',label:'Digital Wallet'},{value:'OTHER',label:'Other'}],acc.type||'CHECKING','— Select —',false))
    )}
    <div id="acc-credit-limit-row" style="${['CREDIT_CARD','CREDIT'].includes(acc.type||'')?'':'display:none'}">
      <label style="display:block;font-size:0.82rem;font-weight:600;color:var(--text-muted);margin-bottom:5px;text-transform:uppercase;letter-spacing:0.04em">Credit Limit</label>
      <div class="input-group">
        <span class="input-group-addon" id="acc-cl-pfx">$</span>
        <input type="number" id="acc-credit-limit" min="0" step="0.01" placeholder="0.00" value="${esc(acc.creditLimit||'')}">
      </div>
    </div>
    <small style="color:var(--text-muted);font-size:0.75rem">Currency cannot be changed after account creation.</small>
    <div class="form-field"><label>Color</label>
      <div class="color-chip-row" id="acc-color-row">
        ${['#EF5350','#FF9800','#FFEE58','#66BB6A','#26C6DA','#42A5F5','#7E57C2','#EC407A'].map(c=>`<span class="color-chip${(acc.colorHex||'#42A5F5')===c?' selected':''}" style="background:${c}" data-color="${c}" onclick="selectColorChip('acc-color','acc-color-row',this)"></span>`).join('')}
        <input type="color" id="acc-color-custom" value="${esc(acc.colorHex||'#42A5F5')}" style="width:32px;height:32px;border:none;border-radius:50%;cursor:pointer;padding:0" oninput="document.getElementById('acc-color').value=this.value;document.querySelectorAll('#acc-color-row .color-chip').forEach(c=>c.classList.remove('selected'))">
      </div>
      <input type="hidden" id="acc-color" value="${esc(acc.colorHex||'#42A5F5')}">
    </div>`;
  openModal(`Edit Account — ${esc(acc.name)}`, body, async () => {
    clearFieldErrors(modalBody);
    const name = document.getElementById('acc-name')?.value.trim();
    const type = document.getElementById('acc-type')?.value;
    if (!name) { showFieldError('acc-name', 'Name is required.'); return; }
    await withLoading(modalConfBtn, async () => {
      const colorHex = document.getElementById('acc-color')?.value || '#42A5F5';
      const payload = { name, structureType: type, colorHex };
      if (['CREDIT_CARD','CREDIT'].includes(type)) {
        const clVal = parseFloat(document.getElementById('acc-credit-limit')?.value || '0');
        if (!isNaN(clVal)) payload.creditLimit = clVal;
      }
      const res = await apiWrite('PUT', `/api/accounts/${encodeURIComponent(id)}`, payload);
      if (res.success) { closeModal(); await warmCache(); renderAccounts(); showToast('Account updated!'); }
      else { showToast('Failed to update: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
  document.getElementById('acc-type')?.addEventListener('change', function() {
    const row = document.getElementById('acc-credit-limit-row');
    if (row) row.style.display = ['CREDIT_CARD','CREDIT'].includes(this.value) ? '' : 'none';
  });
};

window.submitAddAccount = async function () {
  clearFieldErrors(modalBody);
  const name       = document.getElementById('acc-name')?.value.trim();
  const currency   = document.getElementById('acc-currency')?.value.trim();
  const type       = document.getElementById('acc-type')?.value;
  const initBalStr = document.getElementById('acc-init-bal')?.value;
  const initialBalance = initBalStr ? parseFloat(initBalStr) : undefined;
  if (!name)     { showFieldError('acc-name', 'Account name is required.'); return; }
  if (!currency) { showFieldError('acc-currency', 'Currency is required.'); return; }
  await withLoading(modalConfBtn, async () => {
    const colorHex = document.getElementById('acc-color')?.value || '#42A5F5';
    const body = { name, currencyCode: currency, structureType: type, colorHex };
    if (initialBalance !== undefined && !isNaN(initialBalance)) body.initialBalance = initialBalance;
    if (['CREDIT_CARD','CREDIT'].includes(type)) {
      const clVal = parseFloat(document.getElementById('acc-credit-limit')?.value || '0');
      if (!isNaN(clVal)) body.creditLimit = clVal;
    }
    const res = await apiWrite('POST', '/api/accounts', body);
    if (res.success) { closeModal(); await warmCache(); renderAccounts(); showToast('Account created!'); }
    else { showToast('Failed to create account: ' + (res.error || ''), 'error'); }
  });
};

window.deleteAccount = function (id, name) {
  confirmDelete(`Delete account "${name}"? All associated records may also be removed.`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/accounts/${encodeURIComponent(id)}`);
      closeModal();
      if (res.success) { await warmCache(); renderAccounts(); showToast('Account deleted.'); }
      else { showToast('Failed to delete account.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting account.', 'error'); }
  });
};

async function renderAccountDetail(id) {
  if (!id) { navigate('#/accounts'); return; }
  showSpinner();
  try {
    const json = await apiFetch(`/api/accounts/${encodeURIComponent(id)}`);
    if (!json.success) { showError('Failed to load account.'); return; }
    const acc = json.data.account || {};
    const txs = json.data.recentTransactions || [];
    const balClass = (acc.balance || 0) >= 0 ? 'positive' : 'negative';

    let html = `<div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <a class="btn btn-ghost btn-sm" href="#/accounts">← Back</a>
        <h2 class="section-title" style="margin:0">${esc(acc.name)}</h2>
      </div>
      <a class="btn btn-ghost btn-sm" href="#/records?accountId=${esc(acc.id)}&accountName=${encodeURIComponent(acc.name||'')}">All Records</a>
    </div>
    <div class="card-grid" style="margin-bottom:20px">
      <div class="stat-card">
        <div class="stat-label">Current Balance</div>
        <div class="stat-value ${balClass}">${fmt(acc.balance||0, acc.currency)}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Currency</div>
        <div class="stat-value">${esc(acc.currency||'')}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Type</div>
        <div class="stat-value" style="font-size:1rem">${esc(acc.type||'')}</div>
      </div>
    </div>
    <div class="section-sub">Recent Transactions (last 20)</div>`;

    if (!txs.length) {
      html += '<p class="empty-state">No transactions for this account.</p>';
    } else {
      html += `<div class="card"><div class="table-wrap"><table>
        <thead><tr><th>Date</th><th>Title</th><th>Category</th><th>Type</th><th style="text-align:right">Amount</th><th></th></tr></thead><tbody>`;
      txs.forEach(tx => {
        const amtClass = tx.type === 'INCOME' ? 'amount-income' : tx.type === 'EXPENSE' ? 'amount-expense' : '';
        const sign = tx.type === 'EXPENSE' ? '−' : tx.type === 'INCOME' ? '+' : '';
        const badgeCls = tx.type === 'INCOME' ? 'badge-income' : tx.type === 'EXPENSE' ? 'badge-expense' : 'badge-transfer';
        html += `<tr style="cursor:pointer" onclick="openTxDetail(${tx.id})">
          <td style="white-space:nowrap">${fmtDate(tx.date)}</td>
          <td>${esc(tx.title)}</td>
          <td>${tx.categoryName ? esc(tx.categoryName) : '<span style="color:var(--text-muted)">—</span>'}</td>
          <td><span class="badge ${badgeCls}">${prettyEnum(tx.type)}</span></td>
          <td style="text-align:right" class="${amtClass}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</td>
          <td onclick="event.stopPropagation()">
            <button class="btn-icon" title="Edit" onclick="openEditTransaction(${tx.id})">✏️</button>
          </td>
        </tr>`;
      });
      html += '</tbody></table></div></div>';
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading account detail.'); }
}

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
      html += emptyState('', 'No accounts yet.', '+ Add Account', 'openAddAccount()');
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
            <button class="btn btn-ghost btn-sm" onclick="openEditAccount('${esc(acc.id)}')">Edit</button>
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

let _accDetailData = null;
let _accDetailTab  = 'overview';

async function renderAccountDetail(id) {
  if (!id) { navigate('#/accounts'); return; }
  showSpinner();
  try {
    const json = await apiFetch(`/api/accounts/${encodeURIComponent(id)}`);
    if (!json.success) { showError('Failed to load account.'); return; }
    _accDetailData = json.data;
    _accDetailTab  = 'overview';
    _renderAccDetailContent();
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading account detail.'); }
}

window.switchAccDetailTab = function(tab) {
  _accDetailTab = tab;
  _renderAccDetailContent();
};

function _renderAccDetailContent() {
  const acc  = _accDetailData?.account || {};
  const txs  = _accDetailData?.recentTransactions || [];
  const color = acc.colorHex || '#42A5F5';

  // Income / expense totals from recent transactions
  let income = 0, expense = 0;
  txs.forEach(tx => {
    if (tx.type === 'INCOME')  income  += Math.abs(tx.amount || 0);
    if (tx.type === 'EXPENSE') expense += Math.abs(tx.amount || 0);
  });

  const heroHtml = `
    <div class="acc-hero" style="background:${esc(color)}">
      <div class="acc-hero-overlay"></div>
      <div style="position:relative">
        <div class="acc-hero-name">${esc(acc.name)}</div>
        <div class="acc-hero-type">${prettyEnum(acc.type || '')}</div>
        <div class="acc-hero-balance">${fmt(acc.balance || 0, acc.currency)}</div>
        <div class="acc-hero-currency">${esc(acc.currency || '')}</div>
        ${txs.length ? `<div class="acc-hero-stats">
          <div class="acc-hero-stat">
            <div class="acc-hero-stat-label">Income</div>
            <div class="acc-hero-stat-value">+${fmt(income, acc.currency)}</div>
          </div>
          <div class="acc-hero-stat">
            <div class="acc-hero-stat-label">Expense</div>
            <div class="acc-hero-stat-value">−${fmt(expense, acc.currency)}</div>
          </div>
          <div class="acc-hero-stat">
            <div class="acc-hero-stat-label">Transactions</div>
            <div class="acc-hero-stat-value">${txs.length}</div>
          </div>
        </div>` : ''}
        <div class="acc-hero-actions">
          <button class="acc-hero-btn" onclick="openAddTransaction()">+ Transaction</button>
          <button class="acc-hero-btn" onclick="openEditAccount('${esc(acc.id)}')">Edit</button>
          <a class="acc-hero-btn" href="#/records?accountId=${esc(acc.id)}&accountName=${encodeURIComponent(acc.name||'')}">All Records</a>
        </div>
      </div>
    </div>`;

  const tabs = `
    <div class="acc-detail-tabs">
      <div class="acc-detail-tab ${_accDetailTab==='overview'?'active':''}" onclick="switchAccDetailTab('overview')">Overview</div>
      <div class="acc-detail-tab ${_accDetailTab==='transactions'?'active':''}" onclick="switchAccDetailTab('transactions')">Transactions</div>
    </div>`;

  let tabBody = '';
  if (_accDetailTab === 'overview') {
    tabBody += `
      <div class="card" style="margin-bottom:16px">
        <div class="dash-card-header"><span class="dash-card-title">Account Details</span></div>
        <div class="dash-card-body" style="padding:0 18px 16px">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
            <div><div class="stat-label">Type</div><div style="font-weight:600;margin-top:2px">${prettyEnum(acc.type||'')}</div></div>
            <div><div class="stat-label">Currency</div><div style="font-weight:600;margin-top:2px">${esc(acc.currency||'')}</div></div>
            ${acc.creditLimit ? `<div><div class="stat-label">Credit Limit</div><div style="font-weight:600;margin-top:2px">${fmt(acc.creditLimit, acc.currency)}</div></div>` : ''}
          </div>
        </div>
      </div>`;

    if (txs.length) {
      tabBody += `<div class="section-sub" style="margin-bottom:12px">Recent Activity</div>
        <div class="card"><div class="dash-card-body" style="padding:0 0 4px">`;
      txs.slice(0, 5).forEach(tx => {
        const isIncome   = tx.type === 'INCOME';
        const isExpense  = tx.type === 'EXPENSE';
        const typeCls    = isIncome ? 'income' : isExpense ? 'expense' : 'transfer';
        const sign       = isIncome ? '+' : isExpense ? '−' : '';
        tabBody += `<div class="tx-row" onclick="openTxDetail(${tx.id})">
          <div class="tx-dot ${typeCls}"></div>
          <div class="tx-info">
            <div class="tx-name">${esc(tx.title)}</div>
            <div class="tx-sub">${fmtDate(tx.date)}${tx.categoryName ? ' · ' + esc(tx.categoryName) : ''}</div>
          </div>
          <div class="tx-right">
            <div class="tx-amount ${typeCls}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</div>
          </div>
        </div>`;
      });
      tabBody += `</div></div>
        ${txs.length > 5 ? `<div style="text-align:center;margin-top:8px"><button class="btn btn-ghost btn-sm" onclick="switchAccDetailTab('transactions')">View all ${txs.length} transactions →</button></div>` : ''}`;
    } else {
      tabBody += '<p class="empty-state">No recent transactions.</p>';
    }
  } else {
    if (!txs.length) {
      tabBody += '<p class="empty-state">No transactions for this account.</p>';
    } else {
      tabBody += `<div class="card"><div class="dash-card-body" style="padding:0 0 4px">`;
      txs.forEach(tx => {
        const isIncome  = tx.type === 'INCOME';
        const isExpense = tx.type === 'EXPENSE';
        const typeCls   = isIncome ? 'income' : isExpense ? 'expense' : 'transfer';
        const sign      = isIncome ? '+' : isExpense ? '−' : '';
        tabBody += `<div class="tx-row" onclick="openTxDetail(${tx.id})">
          <div class="tx-dot ${typeCls}"></div>
          <div class="tx-info">
            <div class="tx-name">${esc(tx.title)}</div>
            <div class="tx-sub">${fmtDate(tx.date)}${tx.categoryName ? ' · ' + esc(tx.categoryName) : ''}</div>
          </div>
          <div class="tx-right">
            <div class="tx-amount ${typeCls}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency)}</div>
            <div class="tx-actions">
              <button class="btn-icon" title="Edit" onclick="event.stopPropagation();openEditTransaction(${tx.id})">Edit</button>
            </div>
          </div>
        </div>`;
      });
      tabBody += `</div></div>
        <div style="text-align:center;margin-top:12px">
          <a class="btn btn-ghost btn-sm" href="#/records?accountId=${esc(acc.id)}&accountName=${encodeURIComponent(acc.name||'')}">View all records →</a>
        </div>`;
    }
  }

  let html = `<div class="page-header">
    <a class="btn btn-ghost btn-sm" href="#/accounts">← Accounts</a>
  </div>
  ${heroHtml}${tabs}${tabBody}`;

  setContent(html);
}

/* PAGE: Loans + Loan Detail */

/* ── helpers ─────────────────────────────────────────────── */

function _loanAccentVar(loan) {
  return loan.loanType === 'LENT' ? 'var(--accent)' : 'var(--warning)';
}

function _loanIcon(loan) {
  const up = `<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"/></svg>`;
  const dn = `<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"/></svg>`;
  return loan.loanType === 'LENT' ? up : dn;
}

function _loanStatusBadge(status) {
  if (status === 'SETTLED') return '<span class="loan-status-badge settled">Settled</span>';
  return '<span class="loan-status-badge active">Active</span>';
}

function _loanListItem(loan) {
  const isLent = loan.loanType === 'LENT';
  const accentVar = _loanAccentVar(loan);
  const principal = loan.principalAmount || 0;
  const paid = loan.totalPaid || 0;
  const pct = principal > 0 ? Math.min(100, Math.round((paid / principal) * 100)) : 0;
  const partySub = (isLent ? 'Lent to ' : 'Borrowed from ') + esc(loan.lenderOrBorrowerName || '—');

  return `<div class="loan-item" onclick="navigate('#/loans/${esc(loan.id)}')">
    <div class="loan-item-icon" style="color:${accentVar};background:${accentVar}18">${_loanIcon(loan)}</div>
    <div class="loan-item-body">
      <div class="loan-item-header">
        <span class="loan-item-name">${esc(loan.name)}</span>
        <div class="loan-item-trail">
          <span class="loan-item-principal" style="color:${accentVar}">${fmt(principal, loan.currency)}</span>
          ${_loanStatusBadge(loan.status)}
        </div>
      </div>
      <div class="loan-item-sub">${partySub}</div>
      <div class="loan-item-progress-row">
        <div class="loan-item-progress-wrap">
          <div class="loan-item-progress-track">
            <div class="loan-item-progress-fill" style="width:${pct}%;background:${accentVar}"></div>
          </div>
        </div>
        <span class="loan-item-pct" style="color:${accentVar}">${pct}%</span>
      </div>
    </div>
    <div class="loan-item-actions">
      ${loan.status === 'ACTIVE' ? `<button class="loan-menu-btn" onclick="event.stopPropagation();markLoanSettled('${esc(loan.id)}','${esc(loan.name)}')">Mark Settled</button>` : ''}
      <button class="loan-menu-btn" onclick="event.stopPropagation();openEditLoan('${esc(loan.id)}')">Edit</button>
      <button class="loan-menu-btn danger" onclick="event.stopPropagation();deleteLoan('${esc(loan.id)}','${esc(loan.name)}')">Delete</button>
    </div>
  </div>`;
}

/* ── list page ───────────────────────────────────────────── */

async function renderLoans() {
  showSpinner();
  try {
    const json = await apiFetch('/api/loans');
    if (!json.success) { showError('Failed to load loans.'); return; }
    const loans = json.data || [];

    let html = `<div class="page-header"><h2 class="section-title">Loans</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddLoan()">+ Add Loan</button>
    </div>`;

    if (!loans.length) {
      html += emptyState('', 'No loans yet.', '+ Add Loan', 'openAddLoan()');
      setContent(html);
      return;
    }

    const active = loans.filter(l => l.status !== 'SETTLED');
    const settled = loans.filter(l => l.status === 'SETTLED');

    html += '<div class="loan-list">';
    if (active.length) {
      html += '<div class="loan-section-header">ACTIVE</div>';
      active.forEach(l => { html += _loanListItem(l); });
    }
    if (settled.length) {
      html += '<div class="loan-section-header">SETTLED</div>';
      settled.forEach(l => { html += _loanListItem(l); });
    }
    html += '</div>';

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading loans.'); }
}

/* ── hero card ───────────────────────────────────────────── */

function _loanHeroHtml(loan) {
  const isLent = loan.loanType === 'LENT';
  const accentVar = _loanAccentVar(loan);
  const principal = loan.principalAmount || 0;
  const paid = loan.totalPaid || 0;
  const remaining = loan.remainingBalance || 0;
  const pct = principal > 0 ? Math.min(100, Math.round((paid / principal) * 100)) : 0;
  const party = loan.lenderOrBorrowerName || '—';
  const partyLabel = isLent ? 'LENT TO' : 'BORROWED FROM';

  const bankIcon = `<svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M4 10v7h3v-7H4zm6 0v7h3v-7h-3zm-8 9h19v3H2v-3zm14-9v7h3v-7h-3zM11.5 1L2 6v2h19V6l-9.5-5z"/></svg>`;

  return `<div class="loan-hero" style="--loan-accent:${accentVar}">
    <div class="loan-hero-title-row">
      <div class="loan-hero-icon-wrap" style="background:${accentVar}22;color:${accentVar}">${bankIcon}</div>
      <div>
        <div class="loan-hero-card-title">${esc(loan.name)}</div>
        <div class="loan-hero-card-sub">${isLent ? 'Lent' : 'Borrowed'} &bull; ${loan.status === 'SETTLED' ? 'Settled' : 'Active'}</div>
      </div>
    </div>
    <div class="loan-hero-top">
      <div class="loan-hero-meta">
        <div class="loan-hero-label">REMAINING BALANCE</div>
        <div class="loan-hero-amount" style="color:${accentVar}">${fmt(remaining, loan.currency)}</div>
      </div>
      <div class="loan-hero-badge" style="background:${accentVar}20;color:${accentVar}">${pct}%</div>
    </div>
    <div class="loan-hero-track"><div class="loan-hero-fill" style="width:${pct}%;background:${accentVar}"></div></div>
    <div class="loan-hero-stats">
      <span class="loan-hero-stat-paid">&bull; ${fmt(paid, loan.currency)} settled</span>
      <span class="loan-hero-stat-goal">Goal: ${fmt(principal, loan.currency)}</span>
    </div>
    <div class="loan-hero-chips">
      <div class="loan-hero-chip">
        <div class="loan-hero-chip-label">${partyLabel}</div>
        <div class="loan-hero-chip-val" style="color:${accentVar}">${esc(party)}</div>
      </div>
      ${loan.interestRate ? `<div class="loan-hero-chip">
        <div class="loan-hero-chip-label">INTEREST</div>
        <div class="loan-hero-chip-val">${loan.interestRate}% p.a.</div>
      </div>` : ''}
      ${loan.startDate ? `<div class="loan-hero-chip">
        <div class="loan-hero-chip-label">START</div>
        <div class="loan-hero-chip-val">${fmtDate(loan.startDate)}</div>
      </div>` : ''}
      ${loan.expectedSettlementDate ? `<div class="loan-hero-chip">
        <div class="loan-hero-chip-label">DUE</div>
        <div class="loan-hero-chip-val">${fmtDate(loan.expectedSettlementDate)}</div>
      </div>` : ''}
    </div>
  </div>`;
}

/* ── scope card ──────────────────────────────────────────── */

function _loanScopeHtml(loan) {
  const account = loan.linkedAccountId
    ? (cachedAccounts.find(a => String(a.id) === String(loan.linkedAccountId))?.name || loan.linkedAccountId)
    : null;

  const calIcon = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z"/></svg>`;
  const acctIcon = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z"/></svg>`;
  const rateIcon = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>`;

  let rows = '';
  rows += `<div class="loan-scope-row"><span class="loan-scope-icon">${calIcon}</span><span class="loan-scope-key">TYPE</span><span class="loan-scope-val">${loan.loanType === 'LENT' ? 'Money Lent Out' : 'Money Borrowed'}</span></div>`;
  if (account) {
    rows += `<div class="loan-scope-row"><span class="loan-scope-icon">${acctIcon}</span><span class="loan-scope-key">LINKED ACCOUNT</span><span class="loan-scope-val">${esc(account)}</span></div>`;
  }
  if (loan.interestRate) {
    rows += `<div class="loan-scope-row"><span class="loan-scope-icon">${rateIcon}</span><span class="loan-scope-key">INTEREST RATE</span><span class="loan-scope-val">${loan.interestRate}% per annum</span></div>`;
  }

  return `<div class="loan-scope-card">
    <div class="loan-scope-header">Details</div>
    ${rows}
  </div>`;
}

/* ── amortization schedule ───────────────────────────────── */

function _amortizationHtml(loan) {
  if (!loan.interestRate || !loan.principalAmount) return '';
  const r = loan.interestRate / 100 / 12;
  const principal = loan.principalAmount;

  const settleDateMs = loan.expectedSettlementDate;
  const startDateMs = loan.startDate || Date.now();
  if (!settleDateMs || !startDateMs) return '';
  const months = Math.max(1, Math.round((settleDateMs - startDateMs) / (1000 * 60 * 60 * 24 * 30.44)));

  let monthlyPayment;
  if (r === 0) {
    monthlyPayment = principal / months;
  } else {
    monthlyPayment = (principal * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
  }

  let balance = principal;
  let rows = '';
  const now = new Date(startDateMs);
  for (let i = 1; i <= Math.min(months, 60); i++) {
    const interest = balance * r;
    const principalPart = Math.min(monthlyPayment - interest, balance);
    balance = Math.max(0, balance - principalPart);
    const d = new Date(now);
    d.setMonth(d.getMonth() + i);
    rows += `<tr>
      <td>${d.toLocaleDateString('en-US', {month:'short',year:'numeric'})}</td>
      <td>${fmt(monthlyPayment, loan.currency)}</td>
      <td style="color:var(--expense)">${fmt(interest, loan.currency)}</td>
      <td>${fmt(principalPart, loan.currency)}</td>
      <td>${fmt(Math.max(0, balance), loan.currency)}</td>
    </tr>`;
    if (balance <= 0) break;
  }

  return `<div class="loan-amort-wrap">
    <div class="loan-amort-header" onclick="this.parentElement.classList.toggle('open')">
      <span>Amortization Schedule</span>
      <span class="loan-amort-chevron">▾</span>
    </div>
    <div class="loan-amort-body">
      <div class="table-wrap"><table>
        <thead><tr><th>Month</th><th>Payment</th><th>Interest</th><th>Principal</th><th>Balance</th></tr></thead>
        <tbody>${rows}</tbody>
      </table></div>
      ${months > 60 ? `<p style="color:var(--text-muted);font-size:0.75rem;padding:8px 12px">Showing first 60 of ${months} months</p>` : ''}
    </div>
  </div>`;
}

/* ── activity tab ────────────────────────────────────────── */

function _loanActivityHtml(txs, loan) {
  if (!txs.length) {
    return '<div class="loan-activity-empty">No transactions linked to this loan\'s account.</div>';
  }
  const byDate = {};
  txs.forEach(tx => {
    const key = fmtDate(tx.date);
    if (!byDate[key]) byDate[key] = [];
    byDate[key].push(tx);
  });
  let html = '<div class="loan-activity-list">';
  Object.entries(byDate).forEach(([date, group]) => {
    html += `<div class="loan-activity-group-label">${date}</div>`;
    group.forEach(tx => {
      const isIncome = (tx.amount || 0) >= 0;
      const amtColor = isIncome ? 'var(--income)' : 'var(--expense)';
      const sign = isIncome ? '+' : '';
      const catName = tx.categoryId
        ? (cachedCategories.find(c => String(c.id) === String(tx.categoryId))?.name || '')
        : '';
      html += `<div class="loan-activity-row">
        <div class="loan-activity-info">
          <div class="loan-activity-name">${esc(tx.title || tx.description || 'Transaction')}</div>
          ${catName ? `<div class="loan-activity-sub">${esc(catName)}</div>` : ''}
        </div>
        <div class="loan-activity-amount" style="color:${amtColor}">${sign}${fmt(Math.abs(tx.amount||0), tx.currency || loan.currency)}</div>
      </div>`;
    });
  });
  return html + '</div>';
}

/* ── tab switcher ────────────────────────────────────────── */

window.loanDetailTab = function (tab) {
  document.querySelectorAll('.loan-detail-tab').forEach(el => el.classList.toggle('active', el.dataset.tab === tab));
  document.querySelectorAll('.loan-detail-pane').forEach(el => el.classList.toggle('active', el.dataset.pane === tab));
};

/* ── detail page ─────────────────────────────────────────── */

async function renderLoanDetail(id) {
  if (!id) { navigate('#/loans'); return; }
  showSpinner();
  try {
    const loanRes = await apiFetch(`/api/loans/${encodeURIComponent(id)}`);
    if (!loanRes.success) { showError('Failed to load loan.'); return; }
    const loan = loanRes.data || {};

    let txs = [];
    if (loan.linkedAccountId) {
      try {
        const txRes = await apiFetch(`/api/transactions?accountId=${encodeURIComponent(loan.linkedAccountId)}&limit=200`);
        if (txRes.success) txs = txRes.data || [];
      } catch (_) {}
    }

    const accentVar = _loanAccentVar(loan);

    let html = `<div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <a class="btn btn-ghost btn-sm" href="#/loans">← Back</a>
        <h2 class="section-title" style="margin:0">${esc(loan.name)}</h2>
      </div>
      <div style="display:flex;gap:8px">
        <button class="btn btn-ghost btn-sm" onclick="openEditLoan('${esc(loan.id)}')">Edit</button>
        ${loan.status === 'ACTIVE' ? `<button class="btn btn-primary btn-sm" onclick="openLogRepayment(${loan.id},${loan.principalAmount||0},${loan.remainingBalance||0},${loan.totalPaid||0},'${esc(loan.currency||'USD')}')">+ Repayment</button>` : ''}
      </div>
    </div>

    <div class="loan-detail-pill-nav">
      <div class="loan-detail-tabs">
        <button class="loan-detail-tab active" data-tab="overview" onclick="loanDetailTab('overview')">Overview</button>
        <button class="loan-detail-tab" data-tab="activity" onclick="loanDetailTab('activity')">Activity</button>
      </div>
    </div>

    <div class="loan-detail-pane active" data-pane="overview">
      ${_loanHeroHtml(loan)}
      ${_loanScopeHtml(loan)}
      ${_amortizationHtml(loan)}
    </div>

    <div class="loan-detail-pane" data-pane="activity">
      <div class="loan-activity-header">
        <span>${esc(loan.name)} Transactions</span>
        ${loan.linkedAccountId ? '' : '<span style="color:var(--text-muted);font-size:0.8rem">No linked account</span>'}
      </div>
      ${_loanActivityHtml(txs, loan)}
    </div>`;

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading loan detail.'); }
}

/* ── modals ──────────────────────────────────────────────── */

window.openAddLoan = function () {
  const body = `
    ${formField('Loan Name', '<input type="text" id="ln-name" placeholder="e.g. Car loan">')}
    ${formRow(
      formField('Principal Amount', `<div class="input-group"><span class="input-group-addon" id="ln-principal-pfx">${esc(cachedSettings.baseCurrency||'USD')}</span><input type="number" id="ln-principal" placeholder="0.00" min="0" step="0.01"></div>`),
      formField('Currency', `<input type="text" id="ln-currency" value="${esc(cachedSettings.baseCurrency||'USD')}" maxlength="5" oninput="const p=document.getElementById('ln-principal-pfx');if(p)p.textContent=this.value.toUpperCase()||'—'">`)
    )}
    ${formRow(
      `<div class="form-field"><label data-tip="Borrowed = you owe · Lent = they owe you">Loan Type</label>${buildSelect('ln-type',[{value:'BORROWED',label:'Borrowed'},{value:'LENT',label:'Lent'}],'BORROWED','— Select —',false,"const l=document.getElementById('ln-party-label');if(l)l.textContent=this.value==='BORROWED'?'Lender Name':'Borrower Name'")}</div>`,
      formField('<span id="ln-party-label">Lender Name</span>', '<input type="text" id="ln-party" placeholder="Person or institution">')
    )}
    ${formRow(
      formField('Start Date', '<input type="date" id="ln-start">'),
      formField('Expected Settlement', '<input type="date" id="ln-settle">')
    )}
    ${formField('Interest Rate (% p.a.)', `<div class="input-group"><input type="number" id="ln-rate" placeholder="0.00" min="0" max="100" step="0.01"><span class="input-group-addon">% p.a.</span></div>`)}`;
  openModal('Add Loan', body, submitAddLoan, 'Save Loan');
  modalConfBtn.className = 'btn btn-primary';
};

window.submitAddLoan = async function () {
  const name      = document.getElementById('ln-name')?.value.trim();
  const principal = parseFloat(document.getElementById('ln-principal')?.value || '0');
  const currency  = document.getElementById('ln-currency')?.value.trim();
  const type      = document.getElementById('ln-type')?.value;
  const party     = document.getElementById('ln-party')?.value.trim();
  const start     = document.getElementById('ln-start')?.value;
  const settle    = document.getElementById('ln-settle')?.value;
  const rate      = parseFloat(document.getElementById('ln-rate')?.value || '0');
  clearFieldErrors(modalBody);
  if (!name)                            { showFieldError('ln-name', 'Name is required.'); return; }
  if (isNaN(principal) || principal <= 0) { showFieldError('ln-principal', 'Enter a valid principal amount.'); return; }
  if (start && settle && settle < start) { showFieldError('ln-settle', 'Settlement date must be on or after start date.'); return; }
  await withLoading(modalConfBtn, async () => {
    const res = await apiWrite('POST', '/api/loans', {
      name, principalAmount: principal, currency, loanType: type,
      lenderOrBorrowerName: party || undefined,
      startDate: start || undefined,
      expectedSettlementDate: settle || undefined,
      interestRate: rate || undefined,
    });
    if (res.success) { closeModal(); renderLoans(); showToast('Loan created!'); }
    else { showToast('Failed to create loan: ' + (res.error || ''), 'error'); }
  });
};

window.openEditLoan = async function (id) {
  const resp = await apiFetch(`/api/loans/${encodeURIComponent(id)}`);
  if (!resp.success) { showToast('Could not load loan.', 'error'); return; }
  const loan = resp.data;
  const partyLabel = loan.loanType === 'BORROWED' ? 'Lender Name' : 'Borrower Name';
  const settleVal  = loan.expectedSettlementDate ? new Date(loan.expectedSettlementDate).toISOString().slice(0,10) : '';
  const body = `
    <div class="form-field" style="background:var(--surface2);padding:10px;border-radius:var(--radius-sm);margin-bottom:4px;display:flex;gap:20px">
      <div><div style="font-size:0.75rem;color:var(--text-muted)">Principal</div><div style="font-weight:700">${fmt(loan.principalAmount||0, loan.currency)}</div></div>
      <div><div style="font-size:0.75rem;color:var(--text-muted)">Remaining</div><div style="font-weight:700">${fmt(loan.remainingBalance||0, loan.currency)}</div></div>
      <div><div style="font-size:0.75rem;color:var(--text-muted)">Type</div><div style="font-weight:700">${esc(loan.loanType)}</div></div>
    </div>
    ${formField('Loan Name', `<input type="text" id="ln-name" value="${esc(loan.name)}">`)}
    ${formField(partyLabel, `<input type="text" id="ln-party" value="${esc(loan.lenderOrBorrowerName||'')}" placeholder="${partyLabel}…">`)}
    ${formRow(
      formField('Expected Settlement', `<input type="date" id="ln-settle" value="${esc(settleVal)}">`),
      formField('Interest Rate (% p.a.)', `<div class="input-group"><input type="number" id="ln-rate" value="${loan.interestRate || 0}" min="0" max="100" step="0.01" placeholder="0"><span class="input-group-addon">% p.a.</span></div>`)
    )}
    <small style="color:var(--text-muted);font-size:0.75rem">Principal cannot be changed. Use repayments to adjust balance.</small>`;
  openModal(`Edit Loan — ${esc(loan.name)}`, body, async () => {
    clearFieldErrors(modalBody);
    const name   = document.getElementById('ln-name')?.value.trim();
    const party  = document.getElementById('ln-party')?.value.trim();
    const settle = document.getElementById('ln-settle')?.value;
    const rate   = parseFloat(document.getElementById('ln-rate')?.value || '0');
    if (!name) { showFieldError('ln-name', 'Name is required.'); return; }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/loans/${encodeURIComponent(id)}`, {
        name,
        lenderOrBorrowerName: party || undefined,
        expectedSettlementDate: settle || undefined,
        interestRate: isNaN(rate) ? undefined : rate,
      });
      if (res.success) { closeModal(); renderLoans(); showToast('Loan updated!'); }
      else { showToast('Failed to update: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
};

window.markLoanSettled = function (id, name) {
  openModal(`Mark "${name}" as Settled?`, '<p style="color:var(--text-muted)">This will mark the loan as fully settled. You can still view it under the Settled section.</p>', async () => {
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/loans/${encodeURIComponent(id)}`, { status: 'SETTLED' });
      if (res.success) { closeModal(); renderLoans(); showToast('Loan marked as settled.'); }
      else { showToast('Failed: ' + (res.error||''), 'error'); }
    });
  }, 'Mark Settled');
  modalConfBtn.className = 'btn btn-primary';
};

window.deleteLoan = function (id, name) {
  confirmDelete(`Delete loan "${name}"?`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/loans/${encodeURIComponent(id)}`);
      closeModal();
      if (res.success) { renderLoans(); showToast('Loan deleted.'); }
      else { showToast('Failed to delete loan.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting loan.', 'error'); }
  });
};

window.openLogRepayment = function (loanId, principal=0, remaining=0, paid=0, currency='') {
  const body = `
    <div style="background:var(--surface2);padding:10px 14px;border-radius:var(--radius-sm);margin-bottom:4px;display:flex;gap:20px;flex-wrap:wrap">
      <div><div style="font-size:0.72rem;color:var(--text-muted)">Principal</div><div style="font-weight:700">${fmt(principal, currency)}</div></div>
      <div><div style="font-size:0.72rem;color:var(--text-muted)">Paid</div><div style="font-weight:700;color:var(--success)">${fmt(paid, currency)}</div></div>
      <div><div style="font-size:0.72rem;color:var(--text-muted)">Remaining</div><div style="font-weight:700;color:var(--danger)">${fmt(remaining, currency)}</div></div>
    </div>
    ${formRow(
      formField('Amount', `<div class="input-group"><span class="input-group-addon">${esc(currency)||'$'}</span><input type="number" id="rp-amount" placeholder="0.00" min="0" step="0.01" oninput="const r=${remaining};const v=parseFloat(this.value)||0;const w=document.getElementById('rp-overpay');if(w)w.style.display=v>r&&r>0?'':'none'"></div>`),
      formField('Date', `<input type="date" id="rp-date" value="${new Date().toISOString().slice(0,10)}">`)
    )}
    <div id="rp-overpay" style="display:none;color:var(--warning);font-size:0.8rem;margin-top:-8px">Exceeds remaining balance — overpayment will settle the loan.</div>
    ${formField('Account (debited)', buildSelect('rp-account',[{value:'',label:'— None —'}].concat(cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`}))),'','— None —',true))}
    ${formField('Note (optional)', '<input type="text" id="rp-note" placeholder="e.g. Monthly payment">')}`;
  openModal('Log Repayment', body, () => submitLogRepayment(loanId), 'Save');
  modalConfBtn.className = 'btn btn-primary';
};

window.submitLogRepayment = async function (loanId) {
  clearFieldErrors(modalBody);
  const amount    = parseFloat(document.getElementById('rp-amount')?.value || '0');
  const date      = document.getElementById('rp-date')?.value;
  const note      = document.getElementById('rp-note')?.value.trim();
  const accountId = document.getElementById('rp-account')?.value || undefined;
  if (isNaN(amount) || amount <= 0) { showFieldError('rp-amount', 'Enter a valid amount.'); return; }
  await withLoading(modalConfBtn, async () => {
    const res = await apiWrite('POST', `/api/loans/${encodeURIComponent(loanId)}/payments`, {
      amount, date: date ? new Date(date).getTime() : Date.now(),
      note: note || undefined, accountId
    });
    if (res.success) { closeModal(); renderLoanDetail(loanId); showToast('Repayment logged!'); }
    else { showToast('Failed to log repayment.', 'error'); }
  });
};

window.deleteRepayment = function (loanId, paymentId) {
  confirmDelete('Delete this repayment?', async () => {
    try {
      const res = await apiWrite('DELETE', `/api/loans/${encodeURIComponent(loanId)}/payments/${encodeURIComponent(paymentId)}`);
      closeModal();
      if (res.success) { renderLoanDetail(loanId); showToast('Repayment deleted.'); }
      else { showToast('Failed to delete repayment.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting repayment.', 'error'); }
  });
};

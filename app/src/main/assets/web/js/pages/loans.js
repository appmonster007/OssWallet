/* PAGE: Loans + Loan Detail */

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
      html += emptyState('🏦', 'No loans yet.', '+ Add Loan', 'openAddLoan()');
    } else {
      html += '<div class="card-grid">';
      loans.forEach(loan => {
        const repaidPct = Math.round(((loan.totalPaid || 0) / (loan.principalAmount || 1)) * 100);
        const typeBadge = loan.loanType === 'LENT' ? 'badge-lent' : 'badge-borrowed';
        const statusBadge = loan.status === 'SETTLED' ? 'badge-settled' : 'badge-active';
        html += `<div class="stat-card">
          <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:8px">
            <div>
              <div style="font-weight:700">${esc(loan.name)}</div>
              <div style="font-size:0.8rem;color:var(--text-muted);margin-top:2px">${esc(loan.lenderOrBorrowerName || '')}</div>
            </div>
            <div style="display:flex;flex-direction:column;align-items:flex-end;gap:4px">
              <span class="badge ${typeBadge}">${esc(loan.loanType)}</span>
              <span class="badge ${statusBadge}">${esc(loan.status)}</span>
            </div>
          </div>
          <div style="font-size:0.82rem;color:var(--text-muted);margin-bottom:2px">Principal</div>
          <div style="font-weight:700;font-size:1.1rem;margin-bottom:8px">${fmt(loan.principalAmount||0, loan.currency)}</div>
          ${progressBar(repaidPct, repaidPct >= 100 ? 'progress-green' : 'progress-orange')}
          <div style="display:flex;justify-content:space-between;font-size:0.78rem;color:var(--text-muted);margin-top:4px">
            <span>Paid: ${fmt(loan.totalPaid||0,loan.currency)}</span>
            <span>Left: ${fmt(loan.remainingBalance||0,loan.currency)}</span>
          </div>
          ${loan.interestAccrued ? `<div style="font-size:0.78rem;color:var(--warning);margin-top:2px">Interest: ${fmt(loan.interestAccrued, loan.currency)}</div>` : ''}
          <div style="font-size:0.78rem;color:var(--text-muted);margin-top:4px">
            ${loan.startDate ? `Start: ${fmtDate(loan.startDate)}` : ''}
            ${loan.expectedSettlementDate ? ` &bull; Due: ${fmtDate(loan.expectedSettlementDate)}` : ''}
          </div>
          <div style="margin-top:12px;display:flex;gap:6px;flex-wrap:wrap">
            <a class="btn btn-primary btn-sm" href="#/loans/${esc(loan.id)}">Details</a>
            <button class="btn btn-ghost btn-sm" onclick="openEditLoan('${esc(loan.id)}')">✏️ Edit</button>
            <button class="btn btn-sm" style="background:var(--danger);color:#fff" onclick="deleteLoan('${esc(loan.id)}','${esc(loan.name)}')">Delete</button>
          </div>
        </div>`;
      });
      html += '</div>';
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading loans.'); }
}

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
      formField('Interest Rate (% p.a.)', `<div class="input-group"><input type="number" id="ln-rate" value="0" min="0" max="100" step="0.01" placeholder="0"><span class="input-group-addon">% p.a.</span></div>`)
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

async function renderLoanDetail(id) {
  if (!id) { navigate('#/loans'); return; }
  showSpinner();
  try {
    const [loanRes, paymentsRes] = await Promise.all([
      apiFetch(`/api/loans/${encodeURIComponent(id)}`),
      apiFetch(`/api/loans/${encodeURIComponent(id)}/payments`)
    ]);
    if (!loanRes.success) { showError('Failed to load loan.'); return; }
    const loan = loanRes.data || {};
    const payments = paymentsRes.success ? (paymentsRes.data || []) : [];
    const typeBadge = loan.loanType === 'LENT' ? 'badge-lent' : 'badge-borrowed';
    const statusBadge = loan.status === 'SETTLED' ? 'badge-settled' : 'badge-active';
    const repaidPct = Math.round(((loan.totalPaid||0)/(loan.principalAmount||1))*100);

    let html = `<div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <a class="btn btn-ghost btn-sm" href="#/loans">← Back</a>
        <h2 class="section-title" style="margin:0">${esc(loan.name)}</h2>
      </div>
      <div style="display:flex;gap:6px">
        <span class="badge ${typeBadge}">${esc(loan.loanType)}</span>
        <span class="badge ${statusBadge}">${esc(loan.status)}</span>
      </div>
    </div>
    <div class="card-grid" style="margin-bottom:20px">
      <div class="stat-card">
        <div class="stat-label">Principal</div>
        <div class="stat-value">${fmt(loan.principalAmount||0, loan.currency)}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Total Paid</div>
        <div class="stat-value" style="color:var(--success)">${fmt(loan.totalPaid||0, loan.currency)}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Remaining</div>
        <div class="stat-value" style="color:var(--danger)">${fmt(loan.remainingBalance||0, loan.currency)}</div>
      </div>
      ${loan.interestAccrued ? `<div class="stat-card">
        <div class="stat-label">Interest Accrued</div>
        <div class="stat-value" style="color:var(--warning)">${fmt(loan.interestAccrued, loan.currency)}</div>
      </div>` : ''}
    </div>
    <div class="card" style="max-width:520px;margin-bottom:20px">
      ${progressBar(repaidPct, repaidPct>=100?'progress-green':'progress-orange')}
      <div style="display:flex;justify-content:space-between;font-size:0.78rem;color:var(--text-muted);margin-top:4px">
        <span>Repaid: ${repaidPct}%</span>
        ${loan.lenderOrBorrowerName ? `<span>${esc(loan.lenderOrBorrowerName)}</span>` : ''}
      </div>
      ${loan.startDate ? `<div style="font-size:0.8rem;color:var(--text-muted);margin-top:8px">Start: ${fmtDate(loan.startDate)}</div>` : ''}
      ${loan.expectedSettlementDate ? `<div style="font-size:0.8rem;color:var(--text-muted)">Expected settlement: ${fmtDate(loan.expectedSettlementDate)}</div>` : ''}
    </div>
    <div class="section-sub" style="display:flex;justify-content:space-between;align-items:center">
      <span>Repayment History</span>
      <button class="btn btn-primary btn-sm" onclick="openLogRepayment(${loan.id},${loan.principalAmount||0},${loan.remainingBalance||0},${loan.totalPaid||0},'${esc(loan.currency||'USD')}')">+ Log Repayment</button>
    </div>`;

    if (!payments.length) {
      html += '<p class="empty-state">No repayments logged yet.</p>';
    } else {
      html += `<div class="card"><div class="table-wrap"><table>
        <thead><tr><th>Date</th><th>Amount</th><th>Note</th><th></th></tr></thead><tbody>`;
      payments.forEach(p => {
        html += `<tr>
          <td style="white-space:nowrap">${fmtDate(p.date)}</td>
          <td>${fmt(p.amount||0, loan.currency)}</td>
          <td>${p.note ? esc(p.note) : '<span style="color:var(--text-muted)">—</span>'}</td>
          <td><button class="btn-icon" title="Delete" onclick="deleteRepayment(${loan.id},${p.id})">🗑️</button></td>
        </tr>`;
      });
      html += '</tbody></table></div></div>';
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading loan detail.'); }
}

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
    <div id="rp-overpay" style="display:none;color:var(--warning);font-size:0.8rem;margin-top:-8px">⚠️ Exceeds remaining balance — overpayment will settle the loan.</div>
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

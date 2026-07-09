function renderLoansCard(d) {
  const items = d.items||[];
  if (!items.length) return '<p class="empty-state">No loans.</p>';

  let body = '<div class="card-grid" style="margin-bottom:0">';
  items.forEach(loan => {
    const principal = loan.principalAmount || 0;
    const paid = loan.totalPaid || 0;
    const rp = Math.min(100, Math.round((paid / (principal || 1)) * 100));

    const isLent = loan.loanType === 'LENT';
    const typeBdg = isLent ? 'badge-lent' : 'badge-borrowed';
    const statusBdg = loan.status === 'SETTLED' ? 'badge-settled' : 'badge-active';
    const accentColor = isLent ? 'var(--accent)' : 'var(--warning)';

    body += `<div class="stat-card" style="cursor:pointer" onclick="navigate('#/loans/${esc(loan.id)}')">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px; gap:8px">
        <div style="min-width:0; flex:1">
          <div style="font-weight:700; font-size:0.95rem; white-space:nowrap; overflow:hidden; text-overflow:ellipsis">${esc(loan.name)}</div>
          <div style="font-size:0.75rem; color:var(--text-muted); text-transform:uppercase; margin-top:2px">${isLent ? 'Lent to' : 'Borrowed from'} ${esc(loan.lenderOrBorrowerName||'Unknown')}</div>
        </div>
        <div style="display:flex; flex-direction:column; gap:4px; align-items:flex-end">
          <span class="badge ${typeBdg}">${isLent ? 'LENT' : 'BORROWED'}</span>
          <span class="badge ${statusBdg}">${esc(loan.status)}</span>
        </div>
      </div>

      <div style="font-weight:800; font-size:1.1rem; margin-bottom:8px">${fmt(principal, loan.currency)}</div>

      <div class="progress-track" style="height:3px; background:rgba(0,0,0,0.05)">
        <div class="progress-fill" style="width:${rp}%; background:${accentColor}"></div>
      </div>

      <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:var(--text-muted); margin-top:6px">
        <span>Paid: ${fmt(paid, loan.currency)}</span>
        <span style="font-weight:600; color:${accentColor}">${rp}%</span>
      </div>

      ${loan.remainingBalance > 0 ? `<div style="font-size:0.72rem; color:var(--text-muted); margin-top:4px">Remaining: <b>${fmt(loan.remainingBalance, loan.currency)}</b></div>` : ''}
      ${loan.interestAccrued > 0 ? `<div style="font-size:0.72rem; color:var(--warning); margin-top:2px">Interest: +${fmt(loan.interestAccrued, loan.currency)}</div>` : ''}
    </div>`;
  });
  return body + '</div>';
}

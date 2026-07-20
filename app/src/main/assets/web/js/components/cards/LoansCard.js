function renderLoansCard(d) {
  const items = (d.items || []).filter(l => l.status !== 'SETTLED');
  if (!items.length) return '<p class="empty-state">No active loans.</p>';

  let html = '';
  items.forEach((loan, i) => {
    const isLent = loan.loanType === 'LENT';
    const accentVar = isLent ? 'var(--accent)' : 'var(--warning)';
    const principal = loan.principalAmount || 0;
    const paid = loan.totalPaid || 0;
    const remaining = loan.remainingBalance || 0;
    const pct = principal > 0 ? Math.min(100, Math.round((paid / principal) * 100)) : 0;
    const partyLabel = isLent ? 'LENT TO' : 'BORROWED FROM';
    const party = loan.lenderOrBorrowerName || '—';

    html += `<div class="loan-card-row" onclick="navigate('#/loans/${esc(loan.id)}')" ${i < items.length - 1 ? '' : 'style="border-bottom:none;padding-bottom:0"'}>
      <div class="loan-card-row-header">
        <div class="loan-card-row-name">${esc(loan.name)}</div>
        <div class="loan-card-row-remaining" style="color:${accentVar}">${fmt(remaining, loan.currency)}</div>
      </div>
      <div class="loan-card-row-party" style="color:${accentVar}">${partyLabel} ${esc(party)}</div>
      <div class="loan-card-row-annotation">
        <span>&bull; ${fmt(paid, loan.currency)} settled</span>
        <span style="margin-left:6px;margin-right:6px">/</span>
        <span>Goal: ${fmt(principal, loan.currency)}</span>
        <span class="loan-card-row-pct" style="background:${accentVar}20;color:${accentVar}">${pct}%</span>
      </div>
      <div class="loan-card-row-track">
        <div class="loan-card-row-fill" style="width:${pct}%;background:${accentVar}"></div>
      </div>
    </div>`;
  });
  return html;
}

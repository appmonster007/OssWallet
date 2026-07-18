function renderPlannedPaymentsCard(d) {
  const upcoming = d.upcoming || [], overdue = d.overdue || [];
  if (!upcoming.length && !overdue.length) return '<p class="empty-state">No upcoming or overdue payments.</p>';

  const renderRow = (p, isOD) => {
    const isIncome   = p.transactionType === 'INCOME';
    const isTransfer = p.transactionType === 'TRANSFER';
    const typeCls    = isIncome ? 'income' : (isTransfer ? 'transfer' : 'expense');
    const amtSign    = isIncome ? '+' : (isTransfer ? '' : '−');

    return `<div class="tx-row${isOD ? ' tx-overdue' : ' tx-planned'}" onclick="navigate('#/planned-payments/${esc(p.id)}')">
      <div class="tx-dot ${typeCls}"></div>

      <div class="tx-info">
        <div class="tx-name">${esc(p.name)}</div>
        <div class="tx-sub" style="display:flex;gap:6px;align-items:center">
          <span style="color:${isOD ? 'var(--expense)' : 'var(--text-muted)'}">${fmtDate(p.nextDueDate)}</span>
          ${esc(p.categoryName || '')}
        </div>
      </div>

      <div class="tx-right">
        <div class="tx-amount ${typeCls}">${amtSign}${fmt(p.amount, p.currency)}</div>
      </div>

      <div class="tx-actions">
        <button class="btn-icon btn-sm" title="Confirm" onclick="event.stopPropagation();confirmPlannedOccurrence('${esc(p.id)}',${p.nextDueDate})">✓</button>
        <button class="btn-icon btn-sm" title="Skip" onclick="event.stopPropagation();skipPlannedOccurrence('${esc(p.id)}',${p.nextDueDate})">»</button>
      </div>
    </div>`;
  };

  let html = '';

  if (overdue.length) {
    html += `<div class="planned-section planned-section-overdue">
      <div class="planned-section-header overdue">Overdue</div>
      ${overdue.map(p => renderRow(p, true)).join('')}
    </div>`;
  }

  if (upcoming.length) {
    html += `<div class="planned-section">
      ${overdue.length ? '<div class="planned-section-header">Upcoming</div>' : ''}
      ${upcoming.map(p => renderRow(p, false)).join('')}
    </div>`;
  }

  return html;
}

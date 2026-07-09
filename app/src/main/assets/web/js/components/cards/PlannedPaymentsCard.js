function renderPlannedPaymentsCard(d) {
  const upcoming = d.upcoming||[], overdue = d.overdue||[];
  if (!upcoming.length && !overdue.length) return '<p class="empty-state">No upcoming or overdue payments.</p>';

  let html = '<div class="planned-payments-list">';

  const renderRow = (p, isOverdue) => {
    const typeCls = isOverdue ? 'amount-expense' : (p.transactionType === 'INCOME' ? 'amount-income' : (p.transactionType === 'TRANSFER' ? 'amount-accent' : 'amount-expense'));

    return `<div class="planned-payment-row" style="padding: 12px 0; border-bottom: 1px dashed var(--border); display: flex; align-items: center; gap: 12px; cursor: pointer" onclick="navigate('#/planned-payments/${esc(p.id)}')">
      <div style="flex: 1; min-width: 0">
        <div style="font-weight: 600; font-size: 0.9rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">${esc(p.name)}</div>
        <div style="font-size: 0.72rem; color: ${isOverdue ? 'var(--danger)' : 'var(--text-muted)'}; text-transform: uppercase; margin-top: 2px">
          ${isOverdue ? 'OVERDUE' : fmtDate(p.nextDueDate)}
        </div>
      </div>

      <div style="text-align: right">
        <div class="${typeCls}" style="font-weight: 700; font-size: 0.95rem">${fmt(p.amount, p.currency)}</div>
        <div style="font-size: 0.7rem; color: var(--text-muted)">${esc(p.categoryName || 'No Category')}</div>
      </div>

      <div class="row-actions" style="display: flex; gap: 4px; margin-left: 8px">
        <button class="btn-icon btn-sm" title="Confirm" onclick="event.stopPropagation(); confirmPlannedOccurrence('${esc(p.id)}', ${p.nextDueDate})">✓</button>
        <button class="btn-icon btn-sm" title="Skip" onclick="event.stopPropagation(); skipPlannedOccurrence('${esc(p.id)}', ${p.nextDueDate})">»</button>
      </div>
    </div>`;
  };

  if (overdue.length) {
    overdue.forEach(p => { html += renderRow(p, true); });
  }

  if (upcoming.length) {
    upcoming.forEach(p => { html += renderRow(p, false); });
  }

  html += '</div>';
  return html;
}

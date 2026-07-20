function renderPlannedPaymentsCard(d) {
  const upcoming = d.upcoming || [], overdue = d.overdue || [];
  if (!upcoming.length && !overdue.length) return '<p class="empty-state">No upcoming or overdue payments.</p>';

  const renderRow = (p, isOD) => {
    const isIncome   = p.transactionType === 'INCOME';
    const isTransfer = p.transactionType === 'TRANSFER';
    const amtColor   = isIncome ? 'var(--income)' : (isTransfer ? 'var(--accent)' : 'var(--expense)');
    const amtSign    = isIncome ? '+' : (isTransfer ? '' : '−');
    const dateColor  = isOD ? 'var(--expense)' : 'var(--accent)';
    const dueLabel   = _ppRelativeDate(p.nextDueDate);

    const repeatIcon = `<svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z"/></svg>`;

    return `<div class="pp-card-row" onclick="navigate('#/planned-payments/${esc(p.id)}')">
      <div class="pp-card-row-main">
        <div class="pp-card-row-name">${esc(p.name)}</div>
        <div class="pp-card-row-sub">
          <span style="color:${dateColor}">${dueLabel}</span>
          ${p.recurrenceDescription ? `<span class="pp-card-row-recur">${repeatIcon} ${esc(p.recurrenceDescription)}</span>` : ''}
        </div>
      </div>
      <div class="pp-card-row-trail">
        <div class="pp-card-row-amount" style="color:${amtColor}">${amtSign}${fmt(p.amount, p.currency)}</div>
        <div class="pp-card-row-actions">
          <button class="pp-card-action-btn confirm" title="Confirm" onclick="event.stopPropagation();confirmPlannedOccurrence('${esc(p.id)}',${p.nextDueDate})">✓</button>
          <button class="pp-card-action-btn skip"    title="Skip"    onclick="event.stopPropagation();skipPlannedOccurrence('${esc(p.id)}',${p.nextDueDate})">››</button>
        </div>
      </div>
    </div>`;
  };

  let html = '';

  if (overdue.length) {
    html += `<div class="pp-card-section-header overdue">OVERDUE (${overdue.length})</div>`;
    html += overdue.map(p => renderRow(p, true)).join('');
  }
  if (upcoming.length) {
    if (overdue.length) html += `<div class="pp-card-section-header">UPCOMING</div>`;
    html += upcoming.map(p => renderRow(p, false)).join('');
  }

  return html;
}

function _ppRelativeDate(ts) {
  if (!ts) return '—';
  const now = Date.now();
  const diff = ts - now;
  const absDays = Math.round(Math.abs(diff) / 86400000);
  if (diff < 0) return absDays === 0 ? 'Due today' : `${absDays}d overdue`;
  if (absDays === 0) return 'Due today';
  if (absDays === 1) return 'Tomorrow';
  if (absDays <= 7) return `In ${absDays} days`;
  return fmtDate(ts);
}

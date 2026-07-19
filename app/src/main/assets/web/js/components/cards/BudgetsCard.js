function renderBudgetsCard(d) {
  const items = d.items || [];
  if (!items.length) return '<p class="empty-state">No budgets.</p>';
  let body = '';
  items.forEach(b => {
    const pct         = Math.min(105, Math.round(b.percentage || 0));
    const isExceeded  = b.alertLevel === 'EXCEEDED';
    const isWarning   = b.alertLevel === 'WARNING';
    const barColor    = isExceeded ? 'var(--expense)' : isWarning ? 'var(--warning)' : 'var(--income)';
    const annotation  = `${fmt(b.spent || 0, b.currency)} / ${fmt(b.limitAmount || 0, b.currency)} · ${pct}%`;

    body += `<div class="bud-card-row" onclick="navigate('#/budgets/${esc(b.id)}')">
      <div class="bud-card-row-header">
        <span class="bud-card-row-name">${esc(b.name)}</span>
        <span class="bud-card-row-remaining" style="color:${isExceeded ? 'var(--expense)' : 'var(--text-muted)'}">${
          isExceeded
            ? '−' + fmt(Math.abs(b.remaining || 0), b.currency)
            : fmt(b.remaining || 0, b.currency) + ' left'
        }</span>
      </div>
      <div class="bud-card-row-annotation">${annotation}</div>
      <div class="bud-card-row-track">
        <div class="bud-card-row-fill" style="width:${Math.min(100,pct)}%;background:${barColor}"></div>
      </div>
    </div>`;
  });
  return body;
}

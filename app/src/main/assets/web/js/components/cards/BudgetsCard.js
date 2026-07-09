function renderBudgetsCard(d) {
  const items = d.items || [];
  if (!items.length) return '<p class="empty-state">No budgets.</p>';
  let body = '';
  items.forEach(b => {
    const pct = Math.min(100, Math.round(b.percentage||0));
    const isExceeded = b.alertLevel==='EXCEEDED';
    const isWarning = b.alertLevel==='WARNING';
    const alertCls = isExceeded ? 'progress-red' : (isWarning ? 'progress-orange' : 'progress-green');

    const archivedBadge = b.isArchived ? '<span class="badge" style="background:var(--border);color:var(--text-muted);font-size:0.65rem;margin-left:4px">Archived</span>' : '';
    const statusBadge = isExceeded
      ? '<span class="badge" style="background:var(--danger);color:#fff;font-size:0.65rem;margin-left:4px">OVER</span>'
      : isWarning
      ? '<span class="badge" style="background:var(--warning);color:#fff;font-size:0.65rem;margin-left:4px">NEAR</span>'
      : '';

    body += `<div style="margin-bottom:16px;cursor:pointer" onclick="navigate('#/budgets/${esc(b.id)}')">
      <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:6px;gap:8px">
        <div style="flex:1;min-width:0">
          <div style="font-weight:600;font-size:0.9rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(b.name)} ${archivedBadge}${statusBadge}</div>
        </div>
        <div style="text-align:right; cursor:pointer" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {categoryIds:${JSON.stringify(b.categoryIds || []).replace(/"/g, '&quot;')}})">
          <span style="font-weight:700;font-size:0.85rem;color:${isExceeded?'var(--danger)':isWarning?'var(--warning)':'var(--accent)'}">${fmt(b.spent||0,b.currency)}</span>
          <span style="color:var(--text-muted);font-size:0.75rem"> / ${fmt(b.limitAmount||0,b.currency)}</span>
        </div>
      </div>
      <div class="progress-track" style="height:3px; background:rgba(0,0,0,0.05); cursor:pointer" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {categoryIds:${JSON.stringify(b.categoryIds || []).replace(/"/g, '&quot;')}})">
        <div class="progress-fill ${alertCls}" style="width:${pct}%"></div>
      </div>
      <div style="display:flex;justify-content:space-between;font-size:0.72rem;color:var(--text-muted);margin-top:4px">
        <span>${pct}% used</span>
        <span>${b.remaining < 0 ? 'Exceeded by ' + fmt(Math.abs(b.remaining), b.currency) : fmt(b.remaining, b.currency) + ' left'}</span>
      </div>
    </div>`;
  });
  return body;
}

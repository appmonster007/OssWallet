function renderRecentActivitiesCard(d) {
  const groups = d.groups || [];
  if (!groups.length) return '<p class="empty-state">No transactions found.</p>';

  let html = '<div class="recent-activities-list">';

  groups.forEach(group => {
    html += `<div class="tx-group">
      <div class="tx-group-header" style="font-size: 0.72rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; padding: 12px 0 6px; border-bottom: 1px solid var(--border); margin-bottom: 8px">
        ${esc(group.label)}
      </div>`;

    group.transactions.forEach(tx => {
      const amtCls = tx.type==='INCOME' ? 'amount-income' : (tx.type==='EXPENSE' ? 'amount-expense' : 'amount-accent');
      const isPlanned = tx.isPlanned;
      const clickAction = isPlanned
        ? `openPlannedActionModal(${tx.paymentId}, ${tx.date}, '${esc(tx.title)}')`
        : `openTxDetail(${tx.id})`;

      html += `<div class="tx-row ${isPlanned ? 'tx-planned' : ''}" style="display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px dashed var(--border); cursor: pointer" onclick="${clickAction}">
        <div class="tx-icon" style="width: 36px; height: 36px; border-radius: 50%; background: var(--surface2); display: flex; align-items: center; justify-content: center; font-size: 1.1rem; flex-shrink: 0">
          ${isPlanned ? '📅' : (tx.type === 'INCOME' ? '↓' : (tx.type === 'EXPENSE' ? '↑' : '↔'))}
        </div>

        <div style="flex: 1; min-width: 0">
          <div style="font-weight: 600; font-size: 0.9rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">${isPlanned ? '🕒 ' : ''}${esc(tx.title)}</div>
          <div style="font-size: 0.75rem; color: var(--text-muted); display: flex; gap: 6px; align-items: center">
            ${tx.categoryName ? `<span style="cursor:pointer; text-decoration:underline" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {categoryIds:[${tx.categoryId}]})">${esc(tx.categoryName)}</span>` : ''}
            ${tx.accountName ? `<span style="opacity: 0.5">•</span><span style="cursor:pointer; text-decoration:underline" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {accountIds:['${esc(tx.accountId)}']})">${esc(tx.accountName)}</span>` : ''}
          </div>
          ${tx.note ? `<div style="font-size: 0.72rem; color: var(--text-muted); opacity: 0.8; margin-top: 2px">${esc(tx.note)}</div>` : ''}
        </div>

        <div style="text-align: right">
          <div class="${amtCls}" style="font-weight: 700; font-size: 0.95rem">${fmt(tx.amount, tx.currency)}</div>
          ${tx.tags && tx.tags.length ? `<div style="font-size: 0.65rem; color: var(--accent); margin-top: 2px">${tx.tags.map(t => `<span style="cursor:pointer" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {tags:['${esc(t)}']})">#${esc(t)}</span>`).join(' ')}</div>` : ''}
        </div>

        ${isPlanned ? `
          <div class="row-actions" style="display: flex; gap: 4px; margin-left: 8px">
            <button class="btn-icon btn-sm" title="Confirm" onclick="event.stopPropagation(); confirmPlannedOccurrence(${tx.paymentId}, ${tx.date})">✓</button>
            <button class="btn-icon btn-sm" title="Skip" onclick="event.stopPropagation(); skipPlannedOccurrence(${tx.paymentId}, ${tx.date})">»</button>
          </div>
        ` : ''}
      </div>`;
    });

    html += '</div>';
  });

  html += '</div>';
  if (d.total > 20) {
    html += `<div style="padding-top: 12px; text-align: right">
      <a href="#/records" class="btn btn-ghost btn-sm">View All ${d.total} Records</a>
    </div>`;
  }
  return html;
}

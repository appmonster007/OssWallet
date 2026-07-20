function renderRecentActivitiesCard(d) {
  const groups = d.groups || [];
  if (!groups.length) return '<p class="empty-state">No transactions found.</p>';

  const showCategory = d.showCategory !== false;
  const showAccount  = d.showAccount  !== false;
  const showNote     = d.showNote     !== false;
  const showTags     = d.showTags     !== false;

  let html = '<div class="recent-activities-list">';

  groups.forEach(group => {
    html += `<div class="tx-group">
      <div class="tx-group-header">${esc(group.label)}</div>`;

    group.transactions.forEach(tx => {
      const isIncome   = tx.type === 'INCOME';
      const isExpense  = tx.type === 'EXPENSE';
      const typeCls    = isIncome ? 'income' : (isExpense ? 'expense' : 'transfer');
      const amtSign    = isIncome ? '+' : (isExpense ? '−' : '');
      const isPlanned  = tx.isPlanned;
      const clickAction = isPlanned
        ? `openPlannedActionModal(${tx.paymentId}, ${tx.date}, '${esc(tx.title)}')`
        : `openTxDetail(${tx.id})`;

      const subtitle = [
        showCategory && tx.categoryName
          ? `<span style="cursor:pointer" onclick="event.stopPropagation();navigateWithFilter('${esc(d.cardId)}',{categoryIds:[${tx.categoryId}]})">${esc(tx.categoryName)}</span>`
          : '',
        showAccount && tx.accountName
          ? `<span style="opacity:.55">•</span><span style="cursor:pointer" onclick="event.stopPropagation();navigateWithFilter('${esc(d.cardId)}',{accountIds:['${esc(tx.accountId)}']})"> ${esc(tx.accountName)}</span>`
          : ''
      ].filter(Boolean).join('');

      html += `<div class="tx-row${isPlanned ? ' tx-planned' : ''}" onclick="${clickAction}">
        <div class="tx-dot ${typeCls}"></div>

        <div class="tx-info">
          <div class="tx-name">${isPlanned ? '🕒 ' : ''}${esc(tx.title)}</div>
          ${subtitle ? `<div class="tx-sub" style="display:flex;gap:4px;align-items:center">${subtitle}</div>` : ''}
          ${showNote && tx.note ? `<div class="tx-sub" style="opacity:.75;margin-top:1px">${esc(tx.note)}</div>` : ''}
          ${showTags && tx.tags && tx.tags.length
            ? `<div class="tx-tags">${tx.tags.map(t => `<span class="tx-tag" style="cursor:pointer" onclick="event.stopPropagation();navigateWithFilter('${esc(d.cardId)}',{tags:['${esc(t)}']})">#${esc(t)}</span>`).join('')}</div>`
            : ''}
        </div>

        <div class="tx-right">
          <div class="tx-amount ${typeCls}">${amtSign}${fmt(tx.amount, tx.currency)}</div>
        </div>

        ${isPlanned ? `<div class="tx-actions">
          <button class="btn-icon btn-sm" title="Confirm" onclick="event.stopPropagation();confirmPlannedOccurrence(${tx.paymentId},${tx.date})">✓</button>
          <button class="btn-icon btn-sm" title="Skip" onclick="event.stopPropagation();skipPlannedOccurrence(${tx.paymentId},${tx.date})">»</button>
        </div>` : ''}
      </div>`;
    });

    html += '</div>';
  });

  html += '</div>';
  if (d.total > 20) {
    html += `<div style="padding-top:12px;text-align:right">
      <a href="#/records" class="btn btn-ghost btn-sm">View All ${d.total} Records</a>
    </div>`;
  }
  return html;
}

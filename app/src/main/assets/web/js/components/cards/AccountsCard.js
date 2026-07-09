function renderAccountsCard(d, baseCur) {
  const accounts = d.accounts || [];
  const bCur = baseCur || cachedSettings.baseCurrency || '';
  const layout = d.layout || 'grid';
  const gridColumns = d.gridColumns || 2;
  const perspective = d.perspective || 'Accounting';

  if (!accounts.length && !d.showAddAccountCard) return '<p class="empty-state">No accounts.</p>';

  let bodyClass = layout === 'grid' ? 'card-grid' : 'dash-row';
  let inlineStyle = layout === 'grid' ? `grid-template-columns: repeat(${gridColumns}, 1fr); gap: 12px; margin-bottom: 0` : 'display: flex; gap: 12px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 0';

  let body = `<div class="${bodyClass}" style="${inlineStyle}">`;

  const incLabel = perspective === 'CashFlow' ? 'IN' : 'INC';
  const expLabel = perspective === 'CashFlow' ? 'OUT' : 'EXP';

  accounts.forEach(acc => {
    const cls = (acc.balance||0) >= 0 ? 'positive' : 'negative';
    const hasTransfer = acc.transfer != null && acc.transfer !== 0;
    const showBase = bCur && acc.currency !== bCur && acc.balanceInBase != null;

    const cardStyle = layout === 'row' ? `min-width: ${gridColumns === 1 ? '280px' : gridColumns === 2 ? '180px' : '120px'}; flex-shrink: 0` : '';

    body += `<div class="stat-card" style="cursor:pointer; ${cardStyle}" onclick="navigate('#/accounts/${esc(acc.id)}')">
      <div style="font-weight:600; margin-bottom:6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis" title="${esc(acc.name)}">${esc(acc.name)}</div>
      <div class="stat-value ${cls}" style="font-size: ${gridColumns === 1 ? '1.3rem' : '1.1rem'}">${fmt(acc.balance||0,acc.currency)}</div>
      ${showBase ? `<div style="font-size:0.75rem;color:var(--text-muted);margin-top:2px">≈ ${fmt(acc.balanceInBase,bCur)}</div>` : ''}

      ${gridColumns < 3 && acc.income != null ? `<div style="font-size:0.75rem;margin-top:8px;display:flex;gap:8px;flex-wrap:wrap">
        <span class="amount-income" title="${incLabel}" style="cursor:pointer" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {accountIds:['${esc(acc.id)}'], transactionTypes:['INCOME']})">${incLabel} ${fmt(acc.income||0,acc.currency)}</span>
        <span class="amount-expense" title="${expLabel}" style="cursor:pointer" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {accountIds:['${esc(acc.id)}'], transactionTypes:['EXPENSE']})">${expLabel} ${fmt(acc.expense||0,acc.currency)}</span>
        ${hasTransfer?`<span style="color:var(--text-muted)" title="Transfer" style="cursor:pointer" onclick="event.stopPropagation(); navigateWithFilter('${esc(d.cardId)}', {accountIds:['${esc(acc.id)}'], transactionTypes:['TRANSFER']})">↔ ${fmt(acc.transfer||0,acc.currency)}</span>`:''}
      </div>` : ''}

      ${gridColumns === 1 ? `<div style="margin-top:10px;display:flex;gap:6px">
        <a class="btn btn-primary btn-sm" href="#/accounts/${esc(acc.id)}" onclick="event.stopPropagation()">Details</a>
        <a class="btn btn-ghost btn-sm" href="#/records?accountId=${esc(acc.id)}&accountName=${encodeURIComponent(acc.name)}" onclick="event.stopPropagation()">Records</a>
      </div>` : ''}
    </div>`;
  });

  if (d.showAddAccountCard) {
    const cardStyle = layout === 'row' ? `min-width: ${gridColumns === 1 ? '280px' : gridColumns === 2 ? '180px' : '120px'}; flex-shrink: 0` : '';
    body += `<div class="stat-card" style="cursor:pointer; display:flex; flex-direction:column; align-items:center; justify-content:center; border: 2px dashed var(--border); background: transparent; ${cardStyle}" onclick="navigate('#/accounts/new')">
      <div style="font-size: 1.5rem; color: var(--text-muted); margin-bottom: 4px">+</div>
      <div style="font-size: 0.75rem; font-weight: 600; color: var(--text-muted); text-transform: uppercase">Add Account</div>
    </div>`;
  }

  return body + '</div>';
}

function renderCurrencyBalancesCard(d) {
  const items = d.items || [];
  const baseCur = d.baseCurrency || cachedSettings.baseCurrency || '';
  const layout = d.layout || 'grid';
  const gridColumns = d.gridColumns || 2;
  const showBase = d.showBaseConversion;

  if (!items.length) return '<p class="empty-state">No currency balances.</p>';

  let bodyClass = layout === 'grid' ? 'currency-grid' : 'dash-row';
  let inlineStyle = layout === 'grid' ? `grid-template-columns: repeat(${gridColumns}, 1fr); gap: 12px` : 'display: flex; gap: 12px; overflow-x: auto; padding-bottom: 8px';

  let body = `<div class="${bodyClass}" style="${inlineStyle}">`;

  items.forEach(item => {
    const cls = (item.balance||0) >= 0 ? 'amount-income' : 'amount-expense';
    const cardStyle = layout === 'row' ? `min-width: 140px; flex-shrink: 0` : '';

    body += `<div class="currency-item" style="cursor:pointer; ${cardStyle}" onclick="navigateWithFilter('${esc(d.cardId)}', {currencies:['${esc(item.currency)}']})">
      <div class="currency-code" style="font-size: 0.72rem; letter-spacing: 0.05em">${esc(item.currency)}</div>
      <div class="currency-balance ${cls}" style="font-size: 1rem; font-weight: 700">${fmt(item.balance||0, item.currency)}</div>
      ${showBase && item.currency !== baseCur && item.balanceInBase != null
        ? `<div class="currency-base" style="font-size: 0.7rem; color: var(--text-muted); opacity: 0.7; margin-top: 2px">≈ ${fmt(item.balanceInBase, baseCur)}</div>` : ''}
    </div>`;
  });
  return body + '</div>';
}

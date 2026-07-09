function renderNetWorthCard(d) {
  const cur = d.currency || cachedSettings.baseCurrency || 'USD';
  const nwCls = (d.netWorth||0) >= 0 ? 'positive' : 'negative';
  const fontSize = d.fontSize || 'Medium';
  const perspective = d.perspective || 'Accounting';
  const showComp = d.showComparison;
  const cardId = d.cardId;

  const heroSize = fontSize === 'Small' ? '1.5rem' : fontSize === 'Large' ? '2rem' : fontSize === 'X-Large' ? '2.6rem' : '1.75rem';

  let html = `<div class="net-worth-card">
    <div class="hero-section" style="margin-bottom: 20px">
      <div class="stat-value ${nwCls}" style="font-size: ${heroSize}; font-weight: 800; letter-spacing: -0.02em; cursor: pointer"
           onclick="navigateWithFilter('${esc(cardId)}')">
        ${fmt(d.netWorth||0,cur)}
      </div>`;

  if (showComp && d.previousNetWorth != null && d.previousNetWorth !== 0) {
    const delta = (d.netWorth||0) - d.previousNetWorth;
    const pct = (delta / Math.abs(d.previousNetWorth) * 100).toFixed(1);
    const pctCls = delta >= 0 ? 'amount-income' : 'amount-expense';
    html += `<div style="font-size: 0.75rem; font-weight: 600; margin-top: 4px">
      <span class="${pctCls}">${delta >= 0 ? '▲' : '▼'} ${delta >= 0 ? '+' : ''}${Math.abs(pct)}%</span>
      <span style="color: var(--text-muted); opacity: 0.7; margin-left: 8px; text-transform: uppercase">vs prior period</span>
    </div>`;
  }

  html += `</div><div class="net-worth-flow-row" style="display: flex; gap: 16px; border-top: 1px dashed var(--border); padding-top: 16px">`;

  const renderCol = (label, current, previous, colorCls, drillDown = {}) => {
    const trend = fmtTrendPct(current, previous);
    return `<div style="flex: 1; min-width: 0; cursor: pointer" onclick="navigateWithFilter('${esc(cardId)}', ${JSON.stringify(drillDown).replace(/"/g, '&quot;')})">
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px">
        <span style="font-size: 0.65rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; opacity: 0.6">${label}</span>
        <span style="font-size: 0.65rem">${trend}</span>
      </div>
      <div class="${colorCls}" style="font-size: 0.9rem; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">
        ${fmt(current, cur)}
      </div>
    </div>`;
  };

  if (perspective === 'Accounting') {
    html += renderCol('INC', d.income, d.previousIncome, 'amount-income', { transactionTypes: ['INCOME'] });
    html += `<div style="width: 1px; background: var(--border); height: 32px"></div>`;
    html += renderCol('EXP', d.expense, d.previousExpense, 'amount-expense', { transactionTypes: ['EXPENSE'] });
    html += `<div style="width: 1px; background: var(--border); height: 32px"></div>`;
    html += renderCol('TRF', d.transfer, d.previousTransfer, 'amount-accent', { transactionTypes: ['TRANSFER'] });
  } else {
    const inflow = (d.income || 0) + (d.transferIn || 0);
    const outflow = (d.expense || 0) + (d.transferOut || 0);
    const prevInflow = d.previousIncome != null ? (d.previousIncome + (d.previousTransferIn || 0)) : null;
    const prevOutflow = d.previousExpense != null ? (d.previousExpense + (d.previousTransferOut || 0)) : null;

    html += renderCol('IN', inflow, prevInflow, 'amount-income', { transactionTypes: ['INCOME', 'TRANSFER_INFLOW'] });
    html += `<div style="width: 1px; background: var(--border); height: 32px"></div>`;
    html += renderCol('OUT', outflow, prevOutflow, 'amount-expense', { transactionTypes: ['EXPENSE', 'TRANSFER_OUTFLOW'] });
  }

  return html + '</div></div>';
}

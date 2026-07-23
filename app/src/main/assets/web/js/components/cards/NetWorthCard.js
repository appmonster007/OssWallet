function renderNetWorthCard(d) {
  const cur = d.currency || cachedSettings.baseCurrency || 'USD';
  const isPos = (d.netWorth||0) >= 0;
  const fontSize = d.fontSize || 'Medium';
  const perspective = d.perspective || 'Accounting';
  const showComp = d.showComparison;
  const cardId = d.cardId;

  const heroSize = fontSize === 'Small' ? '1.6rem' : fontSize === 'Large' ? '2.2rem' : fontSize === 'X-Large' ? '2.8rem' : '1.95rem';
  const heroColor = isPos ? 'var(--income)' : 'var(--expense)';

  let html = `<div>
    <div style="margin-bottom:16px">
      <div style="font-size:${heroSize};font-weight:800;letter-spacing:-0.03em;line-height:1.1;color:${heroColor};cursor:pointer"
           onclick="navigateWithFilter('${esc(cardId)}')">${fmt(d.netWorth||0,cur)}</div>`;

  if (showComp && d.previousNetWorth != null && d.previousNetWorth !== 0) {
    const delta = (d.netWorth||0) - d.previousNetWorth;
    const pct = (delta / Math.abs(d.previousNetWorth) * 100).toFixed(1);
    const trendColor = delta >= 0 ? 'var(--income)' : 'var(--expense)';
    const trendBg = delta >= 0 ? 'var(--income-bg)' : 'var(--expense-bg)';
    html += `<div style="display:inline-flex;align-items:center;gap:4px;margin-top:8px;font-size:0.78rem;font-weight:700;padding:3px 10px;border-radius:20px;background:${trendBg};color:${trendColor}">
      ${delta >= 0 ? '▲' : '▼'} ${delta >= 0 ? '+' : ''}${Math.abs(pct)}% vs prior
    </div>`;
  }

  html += `</div><div class="net-worth-flow-row">`;

  const renderCol = (label, current, previous, color, drillDown = {}) => {
    const trend = fmtTrendPct(current, previous);
    return `<div onclick="navigateWithFilter('${esc(cardId)}', ${JSON.stringify(drillDown).replace(/"/g, '&quot;')})">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
        <span class="stat-label">${label}</span>
        <span style="font-size:0.65rem">${trend}</span>
      </div>
      <div style="font-size:0.92rem;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:${color}">
        ${fmt(current||0, cur)}
      </div>
    </div>`;
  };

  if (perspective === 'Accounting') {
    html += renderCol('INC', d.current.income, d.previous?.income, 'var(--income)', { transactionTypes: ['INCOME'] });
    html += renderCol('EXP', d.current.expense, d.previous?.expense, 'var(--expense)', { transactionTypes: ['EXPENSE'] });
    html += renderCol('TRF', d.current.transfer, d.previous?.transfer, 'var(--transfer)', { transactionTypes: ['TRANSFER'] });
  } else {
    // NetWorth DTO has no transferIn/Out split — approximate as income/expense only
    const inflow  = d.current.inflow  || 0;
    const outflow = d.current.outflow || 0;
    const prevInflow  = d.previous?.inflow  ?? null;
    const prevOutflow = d.previous?.outflow ?? null;
    html += renderCol('IN',  inflow,  prevInflow,  'var(--income)',  { transactionTypes: ['INCOME','TRANSFER_INFLOW'] });
    html += renderCol('OUT', outflow, prevOutflow, 'var(--expense)', { transactionTypes: ['EXPENSE','TRANSFER_OUTFLOW'] });
  }

  return html + '</div></div>';
}

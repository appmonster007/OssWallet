function renderIncomeExpenseBarCard(d) {
  const pts = d.buckets || [];
  const gran = d.granularity || 'MONTH';
  const baseCur = d.baseCurrency || cachedSettings.baseCurrency || '';
  const todayIdx = d.todayBucketIndex;
  const isCumulative = d.isCumulative;

  if (!pts.length) return '<p class="empty-state">No data.</p>';

  const totalIncome  = pts.reduce((s,p)=>s+(p.metrics?.income||0),0);
  const totalExpense = pts.reduce((s,p)=>s+(p.metrics?.expense||0),0);
  const netTotal = totalIncome + totalExpense;
  const netColor = netTotal >= 0 ? 'var(--income)' : 'var(--expense)';

  const summary = `<div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:16px; flex-wrap:wrap; gap:8px">
    <div style="min-width:120px; cursor:pointer" onclick="navigateWithFilter('${esc(d.cardId)}')">
      <div style="font-size:0.7rem; color:var(--text-muted); text-transform:uppercase; font-weight:700; margin-bottom:2px">Net Result</div>
      <div style="font-weight:800;font-size:1.3rem;letter-spacing:-0.01em;color:${netColor}">${fmt(netTotal, baseCur)}</div>
    </div>
    <div style="display:flex; gap:16px; font-size:0.85rem; background:var(--surface2); padding:8px 12px; border-radius:8px">
      <div style="cursor:pointer" onclick="navigateWithFilter('${esc(d.cardId)}', {transactionTypes:['INCOME']})">
        <div style="font-size:0.65rem; color:var(--text-muted); text-transform:uppercase; font-weight:700">Income</div>
        <div class="amount-income">${fmt(totalIncome,baseCur)}</div>
      </div>
      <div style="width:1px; background:var(--border); height:24px; align-self:center"></div>
      <div style="cursor:pointer" onclick="navigateWithFilter('${esc(d.cardId)}', {transactionTypes:['EXPENSE']})">
        <div style="font-size:0.65rem; color:var(--text-muted); text-transform:uppercase; font-weight:700">Expense</div>
        <div class="amount-expense">${fmt(totalExpense,baseCur)}</div>
      </div>
    </div>
  </div>`;

  let processedPoints = [];
  if (isCumulative) {
    let runInc = 0, runExp = 0;
    processedPoints = pts.map(p => {
      runInc += (p.metrics?.income || 0);
      runExp += (p.metrics?.expense || 0);
      return { label: p.label, income: runInc, expense: runExp };
    });
  } else {
    processedPoints = pts.map(b=>({ label:b.label, income:b.metrics?.income||0, expense:b.metrics?.expense||0 }));
  }

  return `${summary}${svgBars(processedPoints,560,140,true,todayIdx)}
    <div style="display:flex; justify-content:space-between; align-items:center; margin-top:12px">
      <div class="chart-legend" style="margin:0">
        <div class="legend-item"><div class="legend-dot" style="background:var(--income)"></div><span>Income</span></div>
        <div class="legend-item"><div class="legend-dot" style="background:var(--expense)"></div><span>Expense</span></div>
        <div class="legend-item"><div class="legend-dot" style="border-top:2px dashed var(--accent);background:none;width:16px;border-radius:0"></div><span>Net</span></div>
      </div>
      <span style="font-size:0.7rem; color:var(--text-muted); text-transform:uppercase; font-weight:700">${isCumulative ? 'Cumulative' : 'Trend'} • ${esc(String(gran||'').toLowerCase())}</span>
    </div>`;
}

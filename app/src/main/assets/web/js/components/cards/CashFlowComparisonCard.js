function renderCashFlowComparisonCard(d, cardId, metric) {
  const current=d.current||[], previous=d.previous||[], lastYear=d.lastYear||[];
  if (!current.length && !previous.length) return '<p class="empty-state">No comparison data.</p>';

  const m = metric || d.selectedMetric || 'net';
  const isCumulative = d.isCumulative;

  const getVal = (p,key) => {
    const metrics = p.metrics || {};
    return key==='income'?(metrics.income||0):key==='expense'?(metrics.expense||0):(metrics.netFlow||0);
  };

  const processSeries = (data, name, color, opacity, isDashed) => {
    let pts = [];
    if (isCumulative) {
      let run = 0;
      pts = [0, ...data.map(p => { run += getVal(p, m); return run; })];
    } else {
      pts = data.map(p => getVal(p, m));
    }
    const labels = isCumulative ? [null, ...data.map(p=>p.label||'')] : data.map(p=>p.label||'');
    return { name, color, opacity, isDashed, labels, points: pts };
  };

  const series = [];
  if (d.showCurrent)  series.push(processSeries(current,  'Current',   'var(--accent)',   1,   false));
  if (d.showPrevious) series.push(processSeries(previous, 'Previous',  'var(--expense)',  0.8, true));
  if (d.showLastYear) series.push(processSeries(lastYear, 'Last Year', 'var(--income)',   0.6, true));

  const toggleHtml = `<div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px">
    <div style="display:flex; gap:6px">
      ${['net','income','expense'].map(opt=>`<button class="btn btn-sm ${m.toLowerCase()===opt?'btn-primary':'btn-ghost'}" style="text-transform:capitalize" onclick="updateCardSetting('${esc(cardId)}', {selectedMetric:'${opt.toUpperCase()}'}, 'CashFlowComparison')">${opt}</button>`).join('')}
    </div>
    <span style="font-size:0.7rem; color:var(--text-muted); text-transform:uppercase; font-weight:700">${isCumulative ? 'Cumulative' : 'Trend'}</span>
  </div>`;

  return `${toggleHtml}${svgLineChart(series,560,160)}
    <div class="chart-legend" style="margin-top:12px">
      ${series.map(s => `<div class="legend-item"><div class="legend-dot" style="background:${esc(s.color)}; ${s.isDashed ? 'border-top:2px dashed '+esc(s.color)+'; background:none; border-radius:0; width:14px' : ''}"></div><span>${esc(s.name)}</span></div>`).join('')}
    </div>`;
}

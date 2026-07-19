const chartNavStack = {};

function renderCategoryBreakdownCard(card, d, isTreemap) {
  const cardId = card.cardId;
  if (!chartNavStack[cardId]) chartNavStack[cardId] = [d.items || []];

  const currentItems = chartNavStack[cardId][chartNavStack[cardId].length - 1];
  const displayCur = d.displayCurrency || cachedSettings.baseCurrency || '';

  window.chartDrillIn = (cid, idx) => {
    const items = chartNavStack[cid][chartNavStack[cid].length - 1];
    const item = items[idx];
    if (item.subcategories && item.subcategories.length > 0) {
      chartNavStack[cid].push(item.subcategories);
      refreshCardUI(cid);
    }
  };

  window.chartGoBack = (cid) => {
    if (chartNavStack[cid].length > 1) {
      chartNavStack[cid].pop();
      refreshCardUI(cid);
    }
  };

  let header = '';
  if (chartNavStack[cardId].length > 1) {
    header = `<button class="btn btn-ghost btn-sm" style="margin-bottom:12px" onclick="chartGoBack('${esc(cardId)}')">← Back</button>`;
  }

  if (isTreemap) {
    return header + renderCategoryTreemapBody(currentItems, displayCur, cardId);
  } else {
    return header + renderCategoryPieBody(currentItems, displayCur, cardId);
  }
}

function renderCategoryPieBody(items, displayCur, cardId) {
  if (!items.length) return '<p class="empty-state">No category data.</p>';
  const cur = displayCur || cachedSettings.baseCurrency || '';
  const sorted = [...items].sort((a,b) => Math.abs(b.amount||0) - Math.abs(a.amount||0));

  const mainItems = sorted.filter(c => (c.percentage || 0) >= 0.02);
  const otherItems = sorted.filter(c => (c.percentage || 0) < 0.02 && Math.abs(c.amount || 0) > 0);

  const finalDisplay = [...mainItems];
  if (otherItems.length > 1) {
    const otherSum = otherItems.reduce((s, c) => s + (c.amount || 0), 0);
    const otherPct = otherItems.reduce((s, c) => s + (c.percentage || 0), 0);
    finalDisplay.push({
      categoryName: 'More Categories',
      amount: otherSum,
      percentage: otherPct,
      colorHex: '#9E9E9E',
      subcategories: otherItems
    });
  } else if (otherItems.length === 1) {
    finalDisplay.push(otherItems[0]);
  }

  const cs = finalDisplay.map((c,i)=>({ label:c.categoryName, value:c.amount, color:c.colorHex||CHART_COLORS[i%CHART_COLORS.length] }));

  let leg = '<div class="chart-legend" style="flex:1">';
  finalDisplay.forEach((c,i)=>{
    const hasSub = c.subcategories && c.subcategories.length > 0;
    const drillAttr = hasSub
      ? `onclick="chartDrillIn('${esc(cardId)}', ${i})"`
      : `onclick="navigateWithFilter('${esc(cardId)}', {categoryIds:[${c.categoryId}]})"`;
    const style = 'cursor:pointer; font-weight:600; ' + (hasSub ? 'text-decoration:underline' : '');

    leg += `<div class="legend-item" ${drillAttr} style="${style}">
      <div class="legend-dot" style="background:${esc(c.colorHex||CHART_COLORS[i%CHART_COLORS.length])}"></div>
      <span style="flex:1">${esc(c.categoryName)}</span>
      <span style="color:var(--text-muted);margin-left:4px">${fmt(c.amount||0,cur)}</span>
      <span style="color:var(--text-muted);margin-left:4px;font-size:0.75rem">${fmtRaw((c.percentage||0)*100,1)}%</span>
    </div>`;
  });
  leg += '</div>';
  return `<div style="display:flex;flex-wrap:wrap;gap:20px;align-items:center">${svgDonut(cs,160)}${leg}</div>`;
}

function renderCategoryTreemapBody(items, displayCur, cardId) {
  if (!items.length) return '<p class="empty-state">No category data.</p>';
  const cur = displayCur || cachedSettings.baseCurrency || '';
  const sorted = [...items].sort((a,b) => Math.abs(b.amount||0) - Math.abs(a.amount||0));

  const tm = sorted.map((c,i)=>({ categoryName:c.categoryName, amount:c.amount, percentage:c.percentage, colorHex:c.colorHex||CHART_COLORS[i%CHART_COLORS.length] }));

  let leg = '<div class="chart-legend" style="margin-top:12px">';
  sorted.forEach((c,i)=>{
    const hasSub = c.subcategories && c.subcategories.length > 0;
    const drillAttr = hasSub
      ? `onclick="chartDrillIn('${esc(cardId)}', ${i})"`
      : `onclick="navigateWithFilter('${esc(cardId)}', {categoryIds:[${c.categoryId}]})"`;
    const style = 'cursor:pointer; font-weight:600; ' + (hasSub ? 'text-decoration:underline' : '');

    leg += `<div class="legend-item" ${drillAttr} style="${style}">
      <div class="legend-dot" style="background:${esc(c.colorHex||CHART_COLORS[i%CHART_COLORS.length])}"></div>
      <span>${esc(c.categoryName)}</span>
      <span style="color:var(--text-muted);margin-left:4px">${fmt(c.amount||0,cur)}</span>
      <span style="color:var(--text-muted);margin-left:4px">${fmtRaw(c.percentage||0,1)}%</span>
    </div>`;
  });
  leg += '</div>';
  return `<div class="treemap-wrap">${svgTreemapChart(tm,560,180)}</div>${leg}`;
}

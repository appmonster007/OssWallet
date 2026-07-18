function renderCategorySpendingTrendCard(d) {
  const buckets = d.buckets||[];
  if (!buckets.length) return '<p class="empty-state">No spending trend data.</p>';

  const isCumulative = d.isCumulative;
  const cur = d.baseCurrency;

  const natureMap = { MUST: 'var(--expense)', NEED: 'var(--accent)', WANT: 'var(--warning)' };
  const processedBuckets = [];
  let runTotals = { MUST: 0, NEED: 0, WANT: 0 };

  buckets.forEach(b => {
    const bucketNatures = { MUST: 0, NEED: 0, WANT: 0 };
    (b.categories||[]).forEach(c => {
      if (bucketNatures[c.nature] !== undefined) bucketNatures[c.nature] += (c.amount || 0);
    });

    if (isCumulative) {
      runTotals.MUST += bucketNatures.MUST;
      runTotals.NEED += bucketNatures.NEED;
      runTotals.WANT += bucketNatures.WANT;
      const sortedCats = Object.entries(runTotals)
        .map(([n, v]) => ({ categoryName: n, amount: v }))
        .sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount));

      processedBuckets.push({
        label: b.label,
        categories: sortedCats
      });
    } else {
      const sortedCats = Object.entries(bucketNatures)
        .map(([n, v]) => ({ categoryName: n, amount: v }))
        .sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount));

      processedBuckets.push({
        label: b.label,
        categories: sortedCats
      });
    }
  });

  const selectedNatures = d.selectedNatures || [];
  const natureFilters = `<div style="display: flex; gap: 8px; margin: 16px 0">
    ${['MUST', 'NEED', 'WANT'].map(n => {
      const active = selectedNatures.includes(n);
      const newNatures = active ? selectedNatures.filter(x => x !== n) : [...selectedNatures, n];
      return `<div class="badge" style="flex:1; text-align:center; padding: 6px; cursor: pointer; background: ${active ? natureMap[n] : 'var(--surface2)'}; color: ${active ? '#fff' : 'var(--text-muted)'}; opacity: ${active ? 1 : 0.5}"
                   onclick="updateCardSetting('${esc(d.cardId)}', {selectedNatures:${JSON.stringify(newNatures).replace(/"/g, '&quot;')}}, 'CategorySpendingTrend')">
        ${prettyEnum(n)}
      </div>`;
    }).join('')}
  </div>`;

  let catList = '';
  if (d.activeCategories && d.activeCategories.length) {
    catList = `<details style="margin-top: 16px">
      <summary style="font-size: 0.75rem; font-weight: 700; color: var(--text-muted); cursor: pointer; text-transform: uppercase; padding: 4px 0">Categories (${d.activeCategories.length})</summary>
      <div style="padding-top: 8px">
        ${d.activeCategories.map(c => `
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid var(--border); cursor: pointer"
               onclick="navigateWithFilter('${esc(d.cardId)}', {categoryIds:[${c.categoryId}]})">
            <div style="display: flex; align-items: center; gap: 8px; min-width: 0; flex: 1">
              <div style="width: 10px; height: 10px; border-radius: 50%; background: ${c.colorHex}; flex-shrink: 0"></div>
              <span style="font-size: 0.85rem; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">${esc(c.categoryName)}</span>
            </div>
            <span style="font-size: 0.85rem; font-weight: 700">${fmt(c.amount, cur)}</span>
          </div>
        `).join('')}
      </div>
    </details>`;
  }

  return `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
      <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; font-weight: 700">${isCumulative ? 'Cumulative' : 'Stack'} By Nature</div>
    </div>
    ${svgStackedBarsChart(processedBuckets, natureMap, 560, 140)}
    ${natureFilters}
    ${catList}
  `;
}

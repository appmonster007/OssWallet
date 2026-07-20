function renderTagUpSetPlotCard(d, baseCur) {
  const sets=d.sets||[], inters=d.intersections||[];
  const cur = baseCur || cachedSettings.baseCurrency || '';
  const metric = d.selectedMetric || 'EXPENSE';
  const cardId = d.cardId;

  if (!sets.length) return '<p class="empty-state">No tag data.</p>';

  const getVal = (s) => metric === 'INCOME' ? s.income : (metric === 'EXPENSE' ? s.expense : s.income + s.expense);
  const maxAmt = Math.max(...sets.map(s=>Math.abs(getVal(s))), 1);

  let body = `<div class="atelier-tabs" style="margin-bottom:16px">
    ${['INCOME', 'EXPENSE', 'NET'].map(m => `<div class="atelier-tab${metric === m ? ' active' : ''}" onclick="updateCardSetting('${esc(cardId)}', {selectedMetric:'${m}'}, 'TagUpSetPlot')">${m}</div>`).join('')}
  </div>`;

  body += '<div style="margin-bottom:16px">';
  sets.forEach(s => {
    const val = getVal(s);
    const pct = Math.min(100, Math.round(Math.abs(val)/maxAmt*100));

    body += `<div style="margin-bottom:12px; cursor:pointer" onclick="navigateWithFilter('${esc(cardId)}', {tags:['${esc(s.tagName)}']})">
      <div style="display:flex;justify-content:space-between;align-items:center;font-size:0.85rem;margin-bottom:4px;gap:8px">
        <span style="font-weight:600;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">#${esc(s.tagName)}</span>
        <span class="${val >= 0 ? 'amount-income' : 'amount-expense'}" style="font-size:0.8rem; font-weight:700">${fmt(val, cur)}</span>
        <span style="color:var(--text-muted);font-size:0.72rem;white-space:nowrap">${s.transactionCount||0} txns</span>
      </div>
      <div class="progress-track" style="height:4px">
        <div class="progress-fill ${val >= 0 ? 'progress-green' : 'progress-red'}" style="width:${pct}%"></div>
      </div>
    </div>`;
  });
  body += '</div>';

  if (inters.length) {
    body += `<div style="font-size:0.7rem; font-weight:800; color:var(--text-muted); text-transform:uppercase; letter-spacing:.1em; margin-bottom:10px">Top Intersections</div>
      <div class="table-wrap"><table style="font-size:0.8rem"><thead><tr><th>Tags</th><th style="text-align:right">${metric}</th><th>Txns</th></tr></thead><tbody>`;
    inters.slice(0, 10).forEach(inter => {
      const iVal = metric === 'INCOME' ? inter.income : (metric === 'EXPENSE' ? inter.expense : inter.income + inter.expense);
      const bdgs=(inter.tags||[]).map(t=>`<span class="badge" style="background:var(--surface2);color:var(--accent);font-size:0.65rem;margin-right:2px;padding:1px 6px">#${esc(t)}</span>`).join('');
      const tagJson = JSON.stringify(inter.tags).replace(/"/g, '&quot;');
      body+=`<tr style="cursor:pointer" onclick="navigateWithFilter('${esc(cardId)}', {tags:${tagJson}})">
        <td>${bdgs}</td>
        <td style="text-align:right; font-weight:700" class="${iVal >= 0 ? 'amount-income' : 'amount-expense'}">${fmt(iVal, cur)}</td>
        <td style="text-align:center">${inter.transactionCount||0}</td>
      </tr>`;
    });
    body += '</tbody></table></div>';
  }
  return body;
}

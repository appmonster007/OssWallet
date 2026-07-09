function renderCashFlowCard(d) {
  const breakdown = d.breakdown || [];
  const baseCur = d.currency || cachedSettings.baseCurrency || '';
  const perspective = d.perspective || 'Accounting';
  const showNet = d.showNetColumn;
  const showBase = d.showBaseConversion;
  const cardId = d.cardId;

  if (!breakdown.length) {
    const netCls = (d.net||0) >= 0 ? 'positive' : 'negative';
    return `<div class="card-grid" style="margin-bottom:0">
      <div class="stat-card" style="cursor:pointer" onclick="navigateWithFilter('${esc(cardId)}', {transactionTypes:['INCOME']})"><div class="stat-label">Income</div><div class="stat-value positive">${fmt(d.income||0,baseCur)}</div></div>
      <div class="stat-card" style="cursor:pointer" onclick="navigateWithFilter('${esc(cardId)}', {transactionTypes:['EXPENSE']})"><div class="stat-label">Expense</div><div class="stat-value negative">${fmt(d.expense||0,baseCur)}</div></div>
      <div class="stat-card" style="cursor:pointer" onclick="navigateWithFilter('${esc(cardId)}')"><div class="stat-label">Net</div><div class="stat-value ${netCls}">${fmt(d.net||0,baseCur)}</div></div>
    </div>`;
  }

  const isAccounting = perspective === 'Accounting';
  const columns = isAccounting
    ? [{label:'INC', key:'income', color:'var(--success)', types:['INCOME']}, {label:'EXP', key:'expense', color:'var(--danger)', types:['EXPENSE']}, {label:'TRF', key:'transfer', color:'var(--accent)', types:['TRANSFER']}]
    : [{label:'INFLOW', key:'inflow', color:'var(--success)', types:['INCOME', 'TRANSFER_INFLOW']}, {label:'OUTFLOW', key:'outflow', color:'var(--danger)', types:['EXPENSE', 'TRANSFER_OUTFLOW']}];

  if (showNet) columns.push({label: isAccounting ? 'NET' : 'NET FLOW', key:'net', color:'var(--text)', types:[]});

  let html = `<div class="table-wrap"><table class="cashflow-table">
    <thead>
      <tr>
        <th style="width:60px">CCY</th>
        ${columns.map(c => `<th style="text-align:right">${c.label}</th>`).join('')}
      </tr>
    </thead>
    <tbody>
      <tr style="background:var(--surface2); font-weight:700">
        <td>Σ</td>
        ${columns.map(c => {
          let val = 0;
          if (c.key === 'income') val = d.income;
          else if (c.key === 'expense') val = d.expense;
          else if (c.key === 'transfer') val = d.transfer;
          else if (c.key === 'net') val = d.net;
          else if (c.key === 'inflow') val = d.income + breakdown.reduce((s,i)=>s+i.transferInInBase, 0);
          else if (c.key === 'outflow') val = d.expense + breakdown.reduce((s,i)=>s+i.transferOutInBase, 0);

          const cls = val >= 0 ? 'amount-income' : 'amount-expense';
          const drill = c.types.length ? { transactionTypes: c.types } : {};
          return `<td style="text-align:right; cursor:pointer" class="${cls}" onclick="navigateWithFilter('${esc(cardId)}', ${JSON.stringify(drill).replace(/"/g, '&quot;')})">${fmt(val, baseCur)}</td>`;
        }).join('')}
      </tr>`;

  breakdown.forEach(item => {
    html += `<tr>
      <td style="font-weight:600">${esc(item.currency)}</td>
      ${columns.map(c => {
        let nativeVal = 0, baseVal = 0;
        if (c.key === 'income') { nativeVal = item.income; baseVal = item.incomeInBase; }
        else if (c.key === 'expense') { nativeVal = item.expense; baseVal = item.expenseInBase; }
        else if (c.key === 'transfer') { nativeVal = item.transfer; baseVal = item.transferInBase; }
        else if (c.key === 'net') {
          nativeVal = item.income + item.expense + item.transfer;
          baseVal = item.incomeInBase + item.expenseInBase + item.transferInBase;
        }
        else if (c.key === 'inflow') {
          nativeVal = item.income + item.transferIn;
          baseVal = item.incomeInBase + item.transferInInBase;
        }
        else if (c.key === 'outflow') {
          nativeVal = item.expense + item.transferOut;
          baseVal = item.expenseInBase + item.transferOutInBase;
        }

        const cls = nativeVal >= 0 ? 'amount-income' : 'amount-expense';
        const drill = { currencies: [item.currency] };
        if (c.types.length) drill.transactionTypes = c.types;

        return `<td style="text-align:right; cursor:pointer" onclick="navigateWithFilter('${esc(cardId)}', ${JSON.stringify(drill).replace(/"/g, '&quot;')})">
          <div class="${cls}" style="font-size:0.85rem">${fmt(nativeVal, item.currency)}</div>
          ${showBase && item.currency !== baseCur ? `<div style="font-size:0.7rem; color:var(--text-muted)">≈ ${fmt(baseVal, baseCur)}</div>` : ''}
        </td>`;
      }).join('')}
    </tr>`;
  });

  html += `</tbody></table></div>`;
  return html;
}

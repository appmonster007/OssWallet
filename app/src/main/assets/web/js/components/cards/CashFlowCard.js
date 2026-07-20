function renderCashFlowCard(d) {
  const breakdown = d.breakdown || [];
  const baseCur = d.currency || cachedSettings.baseCurrency || '';
  const perspective = d.perspective || 'Accounting';
  const showNet = d.showNetColumn;
  const showBase = d.showBaseConversion;
  const cardId = d.cardId;

  if (!breakdown.length) {
    const netColor = (d.net||0) >= 0 ? 'var(--income)' : 'var(--expense)';
    const mkRow = (label, val, color, drill) =>
      `<div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid var(--border2);cursor:pointer"
            onclick="navigateWithFilter('${esc(cardId)}', ${JSON.stringify(drill).replace(/"/g,'&quot;')})">
         <span style="font-size:0.88rem;color:var(--text-muted);font-weight:500">${label}</span>
         <span style="font-size:0.95rem;font-weight:700;color:${color}">${fmt(val,baseCur)}</span>
       </div>`;
    return mkRow('Income', d.income||0, 'var(--income)', {transactionTypes:['INCOME']})
         + mkRow('Expense', d.expense||0, 'var(--expense)', {transactionTypes:['EXPENSE']})
         + mkRow('Transfer', d.transfer||0, 'var(--transfer)', {transactionTypes:['TRANSFER']})
         + `<div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0 0">
              <span style="font-size:0.75rem;font-weight:800;text-transform:uppercase;letter-spacing:.06em;color:var(--text-faint)">Net</span>
              <span style="font-size:1.05rem;font-weight:800;color:${netColor}">${fmt(d.net||0,baseCur)}</span>
            </div>`;
  }

  const isAccounting = perspective === 'Accounting';
  const columns = isAccounting
    ? [{label:'INC', key:'income', color:'var(--income)', types:['INCOME']}, {label:'EXP', key:'expense', color:'var(--expense)', types:['EXPENSE']}, {label:'TRF', key:'transfer', color:'var(--transfer)', types:['TRANSFER']}]
    : [{label:'INFLOW', key:'inflow', color:'var(--income)', types:['INCOME', 'TRANSFER_INFLOW']}, {label:'OUTFLOW', key:'outflow', color:'var(--expense)', types:['EXPENSE', 'TRANSFER_OUTFLOW']}];

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

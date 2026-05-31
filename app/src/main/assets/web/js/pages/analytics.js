/* PAGE: Analytics */

async function renderAnalytics() {
  const now   = new Date();
  const y     = now.getFullYear();
  const m     = String(now.getMonth() + 1).padStart(2,'0');
  const defFrom = `${y}-${m}-01`;
  const lastDay = new Date(y, now.getMonth()+1, 0).getDate();
  const defTo   = `${y}-${m}-${String(lastDay).padStart(2,'0')}`;

  const params      = new URLSearchParams(location.hash.split('?')[1] || '');
  const dateFrom    = params.get('dateFrom') || defFrom;
  const dateTo      = params.get('dateTo')   || defTo;
  const txType      = params.get('txType')   || 'EXPENSE';
  const granularity = params.get('gran')     || 'MONTH';

  showSpinner();
  try {
    const [bdRes, cfRes] = await Promise.all([
      apiFetch(`/api/analytics/category-breakdown?dateFrom=${dateFrom}&dateTo=${dateTo}&type=${txType}`),
      apiFetch(`/api/analytics/cash-flow?dateFrom=${dateFrom}&dateTo=${dateTo}&granularity=${granularity}`),
    ]);
    const breakdown = bdRes.success ? (bdRes.data || []) : [];
    const cashFlow  = cfRes.success ? (cfRes.data || []) : [];

    // Compute savings rate from cash flow
    const totalIncome  = cashFlow.reduce((s,p)=>s+(p.income||0),0);
    const totalExpense = cashFlow.reduce((s,p)=>s+(p.expense||0),0);
    const net = totalIncome - totalExpense;
    const savingsRate = totalIncome > 0 ? ((net / totalIncome) * 100).toFixed(1) : null;
    const savingsRateCls = net >= 0 ? 'positive' : 'negative';

    let html = `<h2 class="section-title">Analytics</h2>

    <div class="card-grid" style="margin-bottom:20px">
      <div class="stat-card">
        <div class="stat-label">Total Income</div>
        <div class="stat-value positive">+${fmtRaw(totalIncome)}</div>
        <div class="stat-sub">${esc(dateFrom)} → ${esc(dateTo)}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Total Expenses</div>
        <div class="stat-value negative">−${fmtRaw(totalExpense)}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Net Cash Flow</div>
        <div class="stat-value ${savingsRateCls}">${net>=0?'+':'−'}${fmtRaw(Math.abs(net))}</div>
      </div>
      ${savingsRate !== null ? `<div class="stat-card">
        <div class="stat-label">Savings Rate</div>
        <div class="stat-value ${savingsRateCls}">${savingsRate}%</div>
        <div class="stat-sub">of income saved</div>
      </div>` : ''}
    </div>

    <div class="card" style="margin-bottom:20px">
      <div style="display:flex;flex-wrap:wrap;align-items:flex-end;gap:16px">
        <div class="form-field" style="margin:0;flex:1;min-width:280px">
          <label>Date Range</label><div class="date-range-wrap"><input type="date" id="an-from" value="${esc(dateFrom)}"><span class="date-range-sep">→</span><input type="date" id="an-to" value="${esc(dateTo)}"></div>
        </div>
        <div class="form-field" style="margin:0;min-width:120px">
          <label>Type</label>
          ${buildSelect('an-type',[{value:'EXPENSE',label:'Expense'},{value:'INCOME',label:'Income'}],txType||'EXPENSE','— Select —',false)}
        </div>
        <div class="form-field" style="margin:0;min-width:140px">
          <label>Granularity</label>
          ${buildSelect('an-gran',[{value:'DAY',label:'Daily'},{value:'WEEK',label:'Weekly'},{value:'MONTH',label:'Monthly'},{value:'YEAR',label:'Yearly'}],granularity||'MONTH','Monthly',false)}
        </div>
        <button class="btn btn-primary btn-sm" onclick="applyAnalyticsFilters()" style="align-self:flex-end;margin-bottom:14px">Apply</button>
      </div>
    </div>`;

    html += '<div class="two-col">';

    // Category donut
    let donutHtml = '';
    if (!breakdown.length) {
      donutHtml = '<p class="empty-state">No category data for this period.</p>';
    } else {
      let top = breakdown.slice(0, 8);
      const rest = breakdown.slice(8);
      if (rest.length) {
        const otherAmt = rest.reduce((s, c) => s + (c.amount || 0), 0);
        top.push({ categoryName: 'Other', amount: otherAmt, percentage: rest.reduce((s,c)=>s+(c.percentage||0),0) });
      }
      const slices = top.map((c, i) => ({
        label: c.categoryName, value: c.amount, color: CHART_COLORS[i % CHART_COLORS.length]
      }));
      let legend = '<div class="chart-legend" style="margin-top:12px">';
      top.forEach((c, i) => {
        legend += `<div class="legend-item">
          <div class="legend-dot" style="background:${esc(CHART_COLORS[i%CHART_COLORS.length])}"></div>
          <span>${esc(c.categoryName)}</span>
          <span style="color:var(--text-muted);margin-left:4px">${fmtRaw(c.amount)} (${fmtRaw(c.percentage || 0, 1)}%)</span>
        </div>`;
      });
      legend += '</div>';
      donutHtml = `<div style="display:flex;justify-content:center">${svgDonut(slices,180)}</div>${legend}`;
    }
    html += `<div class="card"><div class="section-sub">Category Breakdown (${esc(txType)})</div>${donutHtml}</div>`;

    // Cash flow bars + table
    let barHtml = '';
    if (!cashFlow.length) {
      barHtml = '<p class="empty-state">No cash flow data for this period.</p>';
    } else {
      const points = cashFlow.map(p => ({ label: p.label, income: p.income || 0, expense: p.expense || 0 }));
      barHtml = svgBars(points, 400, 140);
      barHtml += `<div class="chart-legend" style="margin-top:8px">
        <div class="legend-item"><div class="legend-dot" style="background:var(--success)"></div><span>Income</span></div>
        <div class="legend-item"><div class="legend-dot" style="background:var(--danger)"></div><span>Expense</span></div>
      </div>`;
      barHtml += '<div style="margin-top:12px"><div class="table-wrap"><table><thead><tr><th>Period</th><th>Income</th><th>Expense</th><th>Net</th><th>Savings %</th></tr></thead><tbody>';
      cashFlow.forEach(p => {
        const pNet = (p.income||0) - (p.expense||0);
        const pRate = (p.income||0) > 0 ? ((pNet / (p.income||1)) * 100).toFixed(1) : '—';
        const netClass = pNet >= 0 ? 'amount-income' : 'amount-expense';
        const sign = pNet >= 0 ? '+' : '−';
        barHtml += `<tr>
          <td>${esc(p.label)}</td>
          <td class="amount-income">+${fmtRaw(p.income||0)}</td>
          <td class="amount-expense">−${fmtRaw(p.expense||0)}</td>
          <td class="${netClass}">${sign}${fmtRaw(Math.abs(pNet))}</td>
          <td style="color:${pNet>=0?'var(--success)':'var(--danger)'}">${typeof pRate==='string'?pRate:pRate+'%'}</td>
        </tr>`;
      });
      barHtml += '</tbody></table></div></div>';
    }
    html += `<div class="card"><div class="section-sub">Cash Flow (${esc(granularity.toLowerCase())})</div>${barHtml}</div>`;

    html += '</div>';
    setContent(html);
    wireDateRange('an-from', 'an-to');
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading analytics.'); }
}

window.applyAnalyticsFilters = function () {
  const from   = document.getElementById('an-from')?.value;
  const to     = document.getElementById('an-to')?.value;
  const txType = document.getElementById('an-type')?.value;
  const gran   = document.getElementById('an-gran')?.value;
  if (from && to && from > to) { showFieldError('an-to', 'End date must be after start date.'); return; }
  const p = new URLSearchParams();
  if (from)   p.set('dateFrom', from);
  if (to)     p.set('dateTo',   to);
  if (txType) p.set('txType',   txType);
  if (gran)   p.set('gran',     gran);
  location.hash = '#/analytics?' + p.toString();
};

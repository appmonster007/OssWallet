/* Formatters and chart primitives */

function fmt(amount, currency) {
  try { return new Intl.NumberFormat(navigator.language, { style: 'currency', currency, minimumFractionDigits: 2 }).format(amount); }
  catch { return Number(amount).toFixed(2) + ' ' + (currency || ''); }
}
function fmtRaw(n, dec) {
  const d = dec ?? cachedSettings.decimalPrecision ?? 2;
  return Number(n).toLocaleString(navigator.language, { minimumFractionDigits: d, maximumFractionDigits: d });
}
function fmtDate(ts) {
  if (!ts) return '—';
  return new Date(ts).toLocaleDateString(navigator.language, { year: 'numeric', month: 'short', day: 'numeric' });
}
function fmtDateTime(ts) {
  if (!ts) return '—';
  return new Date(ts).toLocaleString(navigator.language, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
function esc(str) {
  if (str == null) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

const ENUM_LABELS = {
  INCOME:'Income', EXPENSE:'Expense', TRANSFER:'Transfer',
  CASH:'Cash', CARD:'Card', BANK_TRANSFER:'Bank Transfer',
  DIGITAL_WALLET:'Digital Wallet', OTHER:'Other',
  CLEARED:'Cleared', UNCLEARED:'Uncleared', RECONCILE:'Reconciled',
  MONTHLY:'Monthly', YEARLY:'Yearly', WEEKLY:'Weekly',
  DAILY:'Daily', HALF_YEAR:'Half Year', QUARTERLY:'Quarterly',
  ONE_TIME:'One Time', BORROWED:'Borrowed', LENT:'Lent',
  CHECKING:'Checking', SAVINGS:'Savings', CREDIT_CARD:'Credit Card',
  INVESTMENT:'Investment', CREDIT:'Credit',
  MUST:'Must', NEED:'Need', WANT:'Want',
};
function prettyEnum(val) {
  if (!val) return '—';
  return ENUM_LABELS[val] || val.replace(/_/g,' ').replace(/\b\w/g, c => c.toUpperCase());
}

function progressBar(pct, color) {
  const cls = color || (pct < 80 ? 'progress-green' : pct < 100 ? 'progress-orange' : 'progress-red');
  const w   = Math.min(100, Math.max(0, pct));
  return `<div class="progress-track"><div class="progress-fill ${esc(cls)}" style="width:0" data-pct="${w}"></div></div>`;
}

/* ── Chart helpers ── */

const CHART_COLORS = ['#1976D2','#2e7d32','#c62828','#f57f17','#6a1b9a','#00838f','#ad1457','#4e342e','#37474f','#558b2f'];

function svgDonut(slices, size) {
  size = size || 180;
  if (!slices || !slices.length) return '<p class="empty-state">No data</p>';
  const total = slices.reduce((s, x) => s + (x.value || 0), 0);
  if (total === 0) return '<p class="empty-state">No data</p>';
  const r = 60, cx = size / 2, cy = size / 2, stroke = 28;
  const circ = 2 * Math.PI * r;
  let offset = 0, paths = '';
  slices.forEach((s, i) => {
    const pct  = s.value / total;
    const dash = pct * circ;
    const col  = s.color || CHART_COLORS[i % CHART_COLORS.length];
    paths += `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${esc(col)}"
      stroke-width="${stroke}" stroke-dasharray="${dash.toFixed(2)} ${(circ - dash).toFixed(2)}"
      stroke-dashoffset="${(circ * 0.25 - offset).toFixed(2)}"><title>${esc(s.label)}</title></circle>`;
    offset += dash;
  });
  return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" style="display:block">${paths}</svg>`;
}

function svgBars(points, width, height, showNetLine, todayIdx) {
  width  = width  || 560;
  height = height || 140;
  if (!points || !points.length) return '<p class="empty-state">No data</p>';
  const maxVal  = Math.max(...points.flatMap(p => [p.income || 0, p.expense || 0])) || 1;
  const count   = points.length;
  const pl = 40, pb = 24;
  const usableW = width - pl;
  const slotW   = Math.floor(usableW / count);
  const barW    = Math.max(4, Math.floor(slotW / 2) - 3);
  const chartH  = height - pb;
  let svg = '';
  for (let gi = 0; gi <= 4; gi++) {
    const gy = (chartH * gi / 4).toFixed(1);
    const val = maxVal * (4 - gi) / 4;
    svg += `<line x1="${pl}" y1="${gy}" x2="${width}" y2="${gy}" stroke="var(--border)" stroke-width="1"/>`;
    svg += `<text x="${pl-4}" y="${gy}" text-anchor="end" dominant-baseline="middle" font-size="9" fill="var(--text-muted)">${fmtRaw(val,0)}</text>`;
  }
  const barCenters = [];
  points.forEach((p, i) => {
    const x  = pl + i * slotW;
    const ih = Math.round(((p.income  || 0) / maxVal) * chartH);
    const eh = Math.round(((p.expense || 0) / maxVal) * chartH);
    const cx = x + barW;
    barCenters.push({ x: cx, net: (p.income||0) - (p.expense||0) });
    if (i === todayIdx) svg += `<rect x="${x}" y="0" width="${slotW}" height="${chartH}" fill="var(--primary)" opacity="0.06"/>`;
    svg += `<rect x="${x}" y="${chartH - ih}" width="${barW}" height="${Math.max(1,ih)}" fill="var(--success)" opacity="0.85"><title>Income: ${p.income}</title></rect>`;
    svg += `<rect x="${x + barW + 2}" y="${chartH - eh}" width="${barW}" height="${Math.max(1,eh)}" fill="var(--danger)" opacity="0.85"><title>Expense: ${p.expense}</title></rect>`;
    svg += `<text x="${cx}" y="${height - 4}" text-anchor="middle" font-size="9" fill="var(--text-muted)">${esc(String(p.label).slice(0, 6))}</text>`;
  });
  if (showNetLine && barCenters.length > 1) {
    const maxNet = Math.max(...barCenters.map(c=>Math.abs(c.net)), 1);
    const netPts = barCenters.map(c => {
      const py = (chartH / 2) - (c.net / maxNet) * (chartH / 2);
      return `${c.x.toFixed(1)},${Math.max(0, Math.min(chartH, py)).toFixed(1)}`;
    }).join(' ');
    svg += `<polyline points="${netPts}" fill="none" stroke="var(--primary)" stroke-width="2" stroke-dasharray="4 2" stroke-linecap="round"/>`;
  }
  return `<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" style="overflow:visible;display:block">${svg}</svg>`;
}

function svgLineChart(seriesList, w, h) {
  w = w || 560; h = h || 160;
  if (!seriesList || !seriesList.length) return '<p class="empty-state">No data</p>';
  const first = seriesList.find(s => s.points && s.points.length);
  if (!first || !first.points.length) return '<p class="empty-state">No data</p>';
  const n = first.points.length;
  const allVals = seriesList.flatMap(s => (s.points||[]).filter(v=>v!=null));
  const minVal = Math.min(0, ...allVals);
  const maxVal = Math.max(...allVals, 1);
  const range = maxVal - minVal || 1;
  const pl = 44, pr = 10, pt = 10, pb = 22;
  const cw = w - pl - pr, ch = h - pt - pb;
  let svg = '';
  for (let gi = 0; gi <= 4; gi++) {
    const gy = (pt + ch * gi / 4).toFixed(1);
    const val = minVal + range * (4 - gi) / 4;
    svg += `<line x1="${pl}" y1="${gy}" x2="${w - pr}" y2="${gy}" stroke="var(--border)" stroke-width="1"/>`;
    svg += `<text x="${pl-4}" y="${gy}" text-anchor="end" dominant-baseline="middle" font-size="9" fill="var(--text-muted)">${fmtRaw(val,0)}</text>`;
  }
  seriesList.forEach(s => {
    if (!s.points || !s.points.length) return;
    let segment = [];
    const flushSegment = () => {
      if (segment.length > 1) svg += `<polyline points="${segment.join(' ')}" fill="none" stroke="${esc(s.color||'#1976D2')}" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" opacity="${s.opacity!=null?s.opacity:1}"/>`;
      segment = [];
    };
    s.points.forEach((v, i) => {
      if (v == null) { flushSegment(); return; }
      const px = pl + (n <= 1 ? cw / 2 : i * cw / (n - 1));
      const py = pt + ch - ((v - minVal) / range) * ch;
      segment.push(`${px.toFixed(1)},${py.toFixed(1)}`);
    });
    flushSegment();
  });
  const labels = first.labels || [];
  if (labels.length) {
    const step = Math.max(1, Math.ceil(n / 7));
    for (let i = 0; i < n; i += step) {
      const px = (pl + (n <= 1 ? cw / 2 : i * cw / (n - 1))).toFixed(1);
      svg += `<text x="${px}" y="${h - 5}" text-anchor="middle" font-size="9" fill="var(--text-muted)">${esc(String(labels[i]||'').slice(0,8))}</text>`;
    }
  }
  return `<svg width="100%" viewBox="0 0 ${w} ${h}" style="display:block;overflow:visible" preserveAspectRatio="xMidYMid meet">${svg}</svg>`;
}

function svgStackedBarsChart(buckets, colorMap, w, h) {
  w = w || 560; h = h || 140;
  if (!buckets || !buckets.length) return '<p class="empty-state">No data</p>';
  const maxTotal = Math.max(...buckets.map(b => (b.categories||[]).reduce((s,c)=>s+Math.abs(c.amount||0),0)), 1);
  const count = buckets.length;
  const usableW = w - 40;
  const slotW = Math.floor(usableW / count);
  const barW = Math.max(4, slotW - 4);
  const chartH = h - 24;
  let rects = '';
  buckets.forEach((b, i) => {
    const x = 40 + i * slotW;
    let y = chartH;
    (b.categories||[]).forEach((cat, ci) => {
      const bh = Math.round((Math.abs(cat.amount||0) / maxTotal) * chartH);
      if (bh > 0) {
        const color = colorMap[cat.categoryName] || CHART_COLORS[ci % CHART_COLORS.length];
        y -= bh;
        rects += `<rect x="${x}" y="${y}" width="${barW}" height="${bh}" fill="${esc(color)}" opacity="0.85"><title>${esc(cat.categoryName)}: ${fmtRaw(Math.abs(cat.amount||0))}</title></rect>`;
      }
    });
    rects += `<text x="${(x+barW/2).toFixed(1)}" y="${h-4}" text-anchor="middle" font-size="9" fill="var(--text-muted)">${esc(String(b.label||'').slice(0,6))}</text>`;
  });
  return `<svg width="100%" viewBox="0 0 ${w} ${h}" style="overflow:visible;display:block" preserveAspectRatio="none">${rects}</svg>`;
}

function svgTreemapChart(items, w, h) {
  w = w || 560; h = h || 180;
  if (!items || !items.length) return '<p class="empty-state">No data</p>';
  const sorted = [...items].filter(x=>(x.amount||0)>0).sort((a,b)=>(b.amount||0)-(a.amount||0));
  if (!sorted.length) return '<p class="empty-state">No data</p>';
  const total = sorted.reduce((s,x)=>s+(x.amount||0),0);
  let rects='', texts='', x=0;
  sorted.forEach((item, i) => {
    const pct = (item.amount||0) / total;
    const iw = Math.round(pct * w);
    if (iw < 1) return;
    const color = item.colorHex || CHART_COLORS[i % CHART_COLORS.length];
    rects += `<rect x="${x}" y="0" width="${iw}" height="${h}" fill="${esc(color)}" opacity="0.82" rx="2"><title>${esc(item.categoryName)}: ${fmtRaw(item.amount||0)} (${fmtRaw(item.percentage||pct*100,1)}%)</title></rect>`;
    if (iw > 44) {
      const maxChars = Math.max(1, Math.floor(iw/7));
      const label = item.categoryName.length > maxChars ? item.categoryName.slice(0,maxChars)+'…' : item.categoryName;
      texts += `<text x="${(x+iw/2).toFixed(1)}" y="${(h/2-5).toFixed(1)}" text-anchor="middle" font-size="11" fill="#fff" font-weight="600">${esc(label)}</text>`;
      texts += `<text x="${(x+iw/2).toFixed(1)}" y="${(h/2+11).toFixed(1)}" text-anchor="middle" font-size="10" fill="rgba(255,255,255,0.88)">${fmtRaw(item.percentage||pct*100,1)}%</text>`;
    }
    x += iw;
  });
  return `<svg width="100%" viewBox="0 0 ${w} ${h}" style="display:block;border-radius:6px;overflow:hidden" preserveAspectRatio="none">${rects}${texts}</svg>`;
}

/* ── Dashboard card wrappers ── */

function filterBadge(fs) {
  if (!fs) return '';
  const parts = [];
  const tl = fs.timelineType;
  if (tl && tl !== 'ALL_TIME') {
    const map = { MONTH:'This Month', YEAR:'This Year', TODAY:'Today', WEEK:'This Week', QUARTER:'This Quarter', HALF_YEAR:'Half Year', CUSTOM:'Custom', MONTHLY:'This Month', YEARLY:'This Year', WEEKLY:'This Week', LAST_24H:'Last 24h', LAST_7_DAYS:'Last 7 Days', LAST_30_DAYS:'Last 30 Days', LAST_12_WEEKS:'Last 12 Weeks', LAST_6_MONTHS:'Last 6 Months', LAST_1_YEAR:'Last Year', LAST_5_YEARS:'Last 5 Years' };
    parts.push(map[tl] || tl);
  }
  if (fs.transactionTypes && fs.transactionTypes.length) parts.push(fs.transactionTypes.join('/'));
  else if (fs.txType) parts.push(fs.txType);
  if (fs.accountIds && fs.accountIds.length) parts.push(`${fs.accountIds.length} acct${fs.accountIds.length>1?'s':''}`);
  if (fs.categoryIds && fs.categoryIds.length) parts.push(`${fs.categoryIds.length} cat${fs.categoryIds.length>1?'s':''}`);
  if (fs.currencies && fs.currencies.length) parts.push(fs.currencies.join('/'));
  if (fs.statuses && fs.statuses.length) parts.push(fs.statuses.join('/'));
  if (fs.paymentMethods && fs.paymentMethods.length) parts.push(fs.paymentMethods.join('/'));
  if (fs.tags && fs.tags.length) parts.push(fs.tags.map(t=>`#${t}`).join(' '));
  if (fs.queryNote) parts.push(`note:"${fs.queryNote}"`);
  if (fs.queryPayee) parts.push(`payee:"${fs.queryPayee}"`);
  if (fs.minAmount != null || fs.maxAmount != null) parts.push(`${fs.minAmount||0}–${fs.maxAmount||'∞'}`);
  return parts.length ? `<span class="filter-badge" title="Active filter">⚙️ ${esc(parts.join(' · '))}</span>` : '';
}

function pinnedAccountsWrap(card, bodyHtml, payloadType) {
  const override = cardOverrides[card.cardId];
  const activeBadge = override ? filterBadge(override) : filterBadge(card.filterSummary);
  return `<div class="dash-card-pinned" data-card-id="${esc(card.cardId)}">
    <div class="dash-card-header">
      <span class="dash-card-title">${esc(card.title||'ACCOUNTS')}</span>
      <span class="pinned-label">Pinned</span>
      ${activeBadge}
      <div class="dash-card-actions">
        <button class="btn-icon" title="Filter" onclick="openCardFilterSheet('${esc(card.cardId)}','${esc(payloadType||'')}','${esc(card.cardType||'')}')">⚙️</button>
      </div>
    </div>
    <div class="dash-card-body" id="card-body-${esc(card.cardId)}">${bodyHtml}</div>
  </div>`;
}

function dashCardWrap(card, bodyHtml, payloadType) {
  const override = cardOverrides[card.cardId];
  const activeBadge = override ? filterBadge(override) : filterBadge(card.filterSummary);
  return `<div class="dash-card" data-card-id="${esc(card.cardId)}">
    <div class="dash-card-header">
      <span class="dash-card-title">${esc(card.title||'')}</span>
      ${activeBadge}
      <div class="dash-card-actions">
        <button class="btn-icon" title="Filter" onclick="openCardFilterSheet('${esc(card.cardId)}','${esc(payloadType||'')}','${esc(card.cardType||'')}')">⚙️</button>
        <button class="btn-icon" title="Remove card" onclick="removeDashboardCard('${esc(card.cardId)}')">✕</button>
      </div>
    </div>
    <div class="dash-card-body" id="card-body-${esc(card.cardId)}">${bodyHtml}</div>
  </div>`;
}

function dispatchCardRender(card, d, payloadType) {
  return dashCardWrap(card, dispatchCardBody(card, d, payloadType), payloadType);
}

function dispatchCardBody(card, d, payloadType) {
  switch (payloadType) {
    case 'NetWorth':              return renderNetWorthBody(d);
    case 'CashFlow':              return renderCashFlowBody(d);
    case 'CurrencyBalances':      return renderCurrencyBalancesBody(d);
    case 'Accounts':              return renderAccountsBody(d, d.baseCurrency||'');
    case 'Budgets':               return renderBudgetBody(d);
    case 'PlannedPayments':       return renderPlannedPaymentsBody(d);
    case 'Loans':                 return renderLoansBody(d);
    case 'RecentActivities':      return renderRecentActivitiesBody(d.transactions||[], d.total||0);
    case 'CategoryBreakdown':     return card.cardType==='CATEGORY_TREEMAP' ? renderCategoryTreemapBody(d.items||[], d.displayCurrency||'') : renderCategoryPieBody(d.items||[], d.displayCurrency||'');
    case 'CashFlowComparison':    { if (!window.cachedCfcData) window.cachedCfcData={}; window.cachedCfcData[card.cardId]=d; return renderCashFlowComparisonBody(d, card.cardId); }
    case 'IncomeExpenseBar':      return renderIncomeExpenseBody(d.buckets||[], d.granularity||'MONTH', d.baseCurrency, d.todayBucketIndex);
    case 'CategorySpendingTrend': return renderCategorySpendingTrendBody(d);
    case 'TagUpSetPlot':          return renderTagUpSetPlotBody(d, d.baseCurrency||'');
    case 'Error':                 return `<div class="alert alert-error">${esc(d.message||'Error loading card.')}</div>`;
    default:                      return `<p class="empty-state">Unknown card type: ${esc(d.type||card.cardType||'')}</p>`;
  }
}

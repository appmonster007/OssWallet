/* Formatters and chart primitives */

const CHART_COLORS = ['#4DCE85','#DDE2FA','#E27974','#FBBF24','#93C5FD','#F9A8D4','#A5B4FC','#6EE7B7','#FCA5A5','#86EFAC'];

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

function fmtTrendPct(current, previous) {
  if (previous == null || previous === 0) return '';
  const delta = current - previous;
  const pct = (delta / Math.abs(previous)) * 100;
  const up = delta > 0;
  const color = up ? 'var(--income)' : 'var(--expense)';

  let label = pct.toFixed(1) + '%';
  if (Math.abs(pct) > 999) label = (pct > 0 ? '>999' : '<-999') + '%';

  return `<span style="color:${color};font-weight:700">${up ? '▲' : '▼'} ${up ? '+' : ''}${label}</span>`;
}

function progressBar(pct, color) {
  const cls = color || (pct < 80 ? 'progress-green' : pct < 100 ? 'progress-orange' : 'progress-red');
  const w   = Math.min(100, Math.max(0, pct));
  return `<div class="progress-track"><div class="progress-fill ${esc(cls)}" style="width:0" data-pct="${w}"></div></div>`;
}

/* ── Chart helpers ── */

function svgDonut(slices, size) {
  size = size || 180;
  if (!slices || !slices.length) return '<p class="empty-state">No data</p>';
  const totalMagnitude = slices.reduce((s, x) => s + Math.abs(x.value || 0), 0);
  if (totalMagnitude === 0) return '<p class="empty-state">No data</p>';
  const r = 60, cx = size / 2, cy = size / 2, stroke = 28;
  const circ = 2 * Math.PI * r;
  let offset = 0, paths = '';
  slices.forEach((s, i) => {
    const rawVal = s.value || 0;
    const magVal = Math.abs(rawVal);
    const pct  = magVal / totalMagnitude;
    const dash = pct * circ;
    const col  = s.color || CHART_COLORS[i % CHART_COLORS.length];
    paths += `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${esc(col)}"
      stroke-width="${stroke}" stroke-dasharray="${dash.toFixed(2)} ${(circ - dash).toFixed(2)}"
      stroke-dashoffset="${(circ * 0.25 - offset).toFixed(2)}"><title>${esc(s.label)}: ${fmtRaw(rawVal)}</title></circle>`;
    offset += dash;
  });
  return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" style="display:block">${paths}</svg>`;
}

function svgBars(points, width, height, showNetLine, todayIdx) {
  width  = width  || 560;
  height = height || 140;
  if (!points || !points.length) return '<p class="empty-state">No data</p>';

  const pl = 40, pb = 24, pt = 10;
  const usableW = width - pl;
  const slotW = Math.floor(usableW / points.length);
  const barW = Math.max(4, Math.floor(slotW / 2) - 3);

  const data = points.map((p, i) => ({
    x: pl + i * slotW + barW,
    inc: p.income || 0,
    exp: p.expense || 0,
    net: (p.income || 0) + (p.expense || 0),
    label: p.label
  }));

  const allVals = data.flatMap(d => [d.inc, d.exp, d.net]);
  let minVal = Math.min(0, ...allVals);
  let maxVal = Math.max(0, ...allVals);
  if (minVal === 0 && maxVal === 0) maxVal = 1;
  const range = maxVal - minVal;

  const getY = (val) => pt + (height - pb - pt) * (1 - (val - minVal) / range);
  const zeroY = getY(0);

  let svg = '';
  for (let i = 0; i <= 4; i++) {
    const val = minVal + (range * i / 4);
    const gy = getY(val);
    svg += `<line x1="${pl}" y1="${gy}" x2="${width}" y2="${gy}" stroke="var(--border)" stroke-width="1" opacity="0.3"/>`;
    svg += `<text x="${pl-4}" y="${gy}" text-anchor="end" dominant-baseline="middle" font-size="9" fill="var(--text-muted)">${fmtRaw(val,0)}</text>`;
  }

  data.forEach((p, i) => {
    const x = pl + i * slotW;
    const incY = getY(p.inc);
    const expY = getY(p.exp);
    const cx = p.x;

    if (i === todayIdx) svg += `<rect x="${x}" y="${pt}" width="${slotW}" height="${height-pb-pt}" fill="var(--accent)" opacity="0.08"/>`;

    const incH = Math.abs(zeroY - incY);
    if (incH > 0.5) svg += `<rect x="${x}" y="${Math.min(zeroY, incY)}" width="${barW}" height="${incH}" fill="var(--income)" opacity="0.85" rx="1"><title>Income: ${fmtRaw(p.inc)}</title></rect>`;

    const expH = Math.abs(zeroY - expY);
    if (expH > 0.5) svg += `<rect x="${x + barW + 2}" y="${Math.min(zeroY, expY)}" width="${barW}" height="${expH}" fill="var(--expense)" opacity="0.85" rx="1"><title>Expense: ${fmtRaw(p.exp)}</title></rect>`;

    svg += `<text x="${cx}" y="${height - 4}" text-anchor="middle" font-size="9" fill="var(--text-muted)">${esc(String(p.label).slice(0, 6))}</text>`;
  });

  if (showNetLine && data.length > 1) {
    const netPts = data.slice(0, (todayIdx != null ? todayIdx + 1 : data.length)).map(c => `${c.x.toFixed(1)},${getY(c.net).toFixed(1)}`).join(' ');
    svg += `<polyline points="${netPts}" fill="none" stroke="var(--accent)" stroke-width="2" stroke-dasharray="4 2" stroke-linecap="round" stroke-linejoin="round"/>`;
  }

  return `<svg width="100%" height="${height}" viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" style="overflow:visible;display:block">${svg}</svg>`;
}

function svgLineChart(seriesList, w, h) {
  w = w || 560; h = h || 160;
  if (!seriesList || !seriesList.length) return '<p class="empty-state">No data</p>';
  const first = seriesList.find(s => s.points && s.points.length);
  if (!first || !first.points.length) return '<p class="empty-state">No data</p>';
  const n = first.points.length;
  const allVals = seriesList.flatMap(s => (s.points||[]).filter(v=>v!=null));
  let minVal = Math.min(0, ...allVals);
  let maxVal = Math.max(...allVals, 1);
  if (minVal === maxVal) { minVal -= 1; maxVal += 1; }
  const range = maxVal - minVal || 1;
  const pl = 44, pr = 10, pt = 10, pb = 22;
  const cw = w - pl - pr, ch = h - pt - pb;
  const getY = (v) => pt + ch - ((v - minVal) / range) * ch;

  let svg = '';
  for (let gi = 0; gi <= 4; gi++) {
    const gy = getY(minVal + range * gi / 4);
    svg += `<line x1="${pl}" y1="${gy}" x2="${w - pr}" y2="${gy}" stroke="var(--border)" stroke-width="1" opacity="0.3"/>`;
    svg += `<text x="${pl-4}" y="${gy}" text-anchor="end" dominant-baseline="middle" font-size="9" fill="var(--text-muted)">${fmtRaw(minVal + range * gi / 4,0)}</text>`;
  }
  seriesList.forEach(s => {
    if (!s.points || !s.points.length) return;
    let segment = [];
    const flushSegment = () => {
      if (segment.length > 1) svg += `<polyline points="${segment.join(' ')}" fill="none" stroke="${esc(s.color||'#2979FF')}" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" opacity="${s.opacity!=null?s.opacity:1}" ${s.isDashed ? 'stroke-dasharray="4 2"' : ''}/>`;
      segment = [];
    };
    s.points.forEach((v, i) => {
      if (v == null) { flushSegment(); return; }
      const px = pl + (n <= 1 ? cw / 2 : i * cw / (n - 1));
      const py = getY(v);
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

  let minTotal = 0, maxTotal = 0;
  buckets.forEach(b => {
    let pos = 0, neg = 0;
    (b.categories || []).forEach(c => {
      const val = c.amount || 0;
      if (val > 0) pos += val; else neg += val;
    });
    minTotal = Math.min(minTotal, neg);
    maxTotal = Math.max(maxTotal, pos);
  });
  if (minTotal === 0 && maxTotal === 0) maxTotal = 1;
  const range = maxTotal - minTotal;

  const pl = 40, pb = 24, pt = 10;
  const chartH = h - pb - pt;
  const slotW = Math.floor((w - pl) / buckets.length);
  const barW = Math.max(4, slotW - 4);

  const getY = (val) => pt + chartH * (1 - (val - minTotal) / range);
  const zeroY = getY(0);

  let rects = '';
  for (let i = 0; i <= 4; i++) {
    const val = minTotal + (range * i / 4);
    const gy = getY(val);
    rects += `<line x1="${pl}" y1="${gy}" x2="${w}" y2="${gy}" stroke="var(--border)" stroke-width="1" opacity="0.3"/>`;
    rects += `<text x="${pl-4}" y="${gy}" text-anchor="end" dominant-baseline="middle" font-size="9" fill="var(--text-muted)">${fmtRaw(val,0)}</text>`;
  }

  buckets.forEach((b, i) => {
    const x = pl + i * slotW + 2;
    let posAcc = 0, negAcc = 0;

    (b.categories || []).forEach((cat, ci) => {
      const val = cat.amount || 0;
      if (val === 0) return;

      const bh = Math.abs(getY(val) - zeroY);
      const color = colorMap[cat.categoryName] || CHART_COLORS[ci % CHART_COLORS.length];

      if (val > 0) {
        const y = getY(posAcc + val);
        rects += `<rect x="${x}" y="${y}" width="${barW}" height="${bh}" fill="${esc(color)}" opacity="0.85" rx="1"><title>${esc(cat.categoryName)}: ${fmtRaw(val)}</title></rect>`;
        posAcc += val;
      } else {
        const y = getY(negAcc);
        rects += `<rect x="${x}" y="${y}" width="${barW}" height="${bh}" fill="${esc(color)}" opacity="0.85" rx="1"><title>${esc(cat.categoryName)}: ${fmtRaw(val)}</title></rect>`;
        negAcc += val;
      }
    });
    rects += `<text x="${(x + barW / 2).toFixed(1)}" y="${h - 4}" text-anchor="middle" font-size="9" fill="var(--text-muted)">${esc(String(b.label || '').slice(0, 6))}</text>`;
  });

  return `<svg width="100%" viewBox="0 0 ${w} ${h}" style="overflow:visible;display:block" preserveAspectRatio="none">${rects}</svg>`;
}

function svgTreemapChart(items, w, h) {
  w = w || 560; h = h || 180;
  if (!items || !items.length) return '<p class="empty-state">No data</p>';
  const sorted = [...items].filter(x=>Math.abs(x.amount||0)>0).sort((a,b)=>Math.abs(b.amount||0)-Math.abs(a.amount||0));
  if (!sorted.length) return '<p class="empty-state">No data</p>';
  const total = sorted.reduce((s,x)=>s+Math.abs(x.amount||0),0);
  let rects='', texts='', x=0;
  sorted.forEach((item, i) => {
    const pct = Math.abs(item.amount||0) / total;
    const iw = Math.round(pct * w);
    if (iw < 1) return;
    const color = item.colorHex || CHART_COLORS[i % CHART_COLORS.length];
    rects += `<rect x="${x}" y="0" width="${iw-1}" height="${h}" fill="${esc(color)}" opacity="0.82" rx="2"><title>${esc(item.categoryName)}: ${fmtRaw(item.amount||0)} (${fmtRaw(item.percentage||pct*100,1)}%)</title></rect>`;
    if (iw > 50) {
      const maxChars = Math.max(1, Math.floor(iw/8));
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
        <button class="btn-icon" title="Filter" onclick="openCardFilterSheet('${esc(card.cardId)}','${esc(payloadType||'')}','${esc(card.cardType||'')}')">⚙</button>
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
        <button class="btn-icon" title="Filter" onclick="openCardFilterSheet('${esc(card.cardId)}','${esc(payloadType||'')}','${esc(card.cardType||'')}')">⚙</button>
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
  // Inject cardId into payload so render functions can use it for drill-down navigation
  if (d && !d.cardId) d.cardId = card.cardId;
  switch (payloadType) {
    case 'NetWorth':              return renderNetWorthCard(d);
    case 'CashFlow':              return renderCashFlowCard(d);
    case 'CurrencyBalances':      return renderCurrencyBalancesCard(d);
    case 'Accounts':              return renderAccountsCard(d, d.baseCurrency||'');
    case 'Budgets':               return renderBudgetsCard(d);
    case 'PlannedPayments':       return renderPlannedPaymentsCard(d);
    case 'Loans':                 return renderLoansCard(d);
    case 'RecentActivities':      return renderRecentActivitiesCard(d);
    case 'AccountHero':           return renderAccountHeroCard(d);
    case 'CategoryBreakdown':     return renderCategoryBreakdownCard(card, d, card.cardType==='CATEGORY_TREEMAP');
    case 'CashFlowComparison':    { if (!window.cachedCfcData) window.cachedCfcData={}; window.cachedCfcData[card.cardId]=d; return renderCashFlowComparisonCard(d, card.cardId); }
    case 'IncomeExpenseBar':      return renderIncomeExpenseBarCard(d);
    case 'CategorySpendingTrend': return renderCategorySpendingTrendCard(d);
    case 'TagUpSetPlot':          return renderTagUpSetPlotCard(d, d.baseCurrency||'');
    case 'Error':                 return `<div class="alert alert-error">${esc(d.message||'Error loading card.')}</div>`;
    default:                      return `<p class="empty-state">Unknown card type: ${esc(d.type||card.cardType||'')}</p>`;
  }
}

function refreshCardUI(cardId) {
  const el = document.getElementById('card-body-' + cardId);
  if (!el) return;
  const dataSrc = window.lastDashboardData || (window.lastAnalyticsResponse ? window.lastAnalyticsResponse.cards : null);
  if (dataSrc) {
    const card = dataSrc.find(c => c.cardId === cardId);
    if (card) {
      const d = card.data;
      const pt = d.type ? d.type.split('.').pop() : '';
      el.innerHTML = dispatchCardBody(card, d, pt);
    }
  }
}

async function refreshCardContent(cardId, payloadType, cardType) {
  try {
    const res = await apiFetch(`/api/dashboard/cards/${cardId}`);
    if (res.success) {
      const card = res.data;
      const d = card.data || {};
      const el = document.getElementById('card-body-' + cardId);
      if (el) el.innerHTML = dispatchCardBody(card, d, payloadType || d.type?.split('.').pop());

      // Update filter badge if any
      const badgeEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-header .filter-badge`);
      const newBadge = filterBadge(card.filterSummary);
      if (badgeEl) { badgeEl.outerHTML = newBadge || ''; }
      else if (newBadge) {
        const titleEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-title`);
        if (titleEl) titleEl.insertAdjacentHTML('afterend', newBadge);
      }
    }
  } catch (e) { console.error('Failed to refresh card', cardId, e); }
}

async function updateCardSetting(cardId, settings, payloadType) {
  try {
    const res = await apiWrite('PUT', `/api/dashboard/cards/${cardId}`, settings);
    if (res.success) {
      await refreshCardContent(cardId, payloadType);
    } else {
      showToast('Failed to update card: ' + (res.error || ''), 'error');
    }
  } catch (e) { console.error('Failed to update card setting', cardId, settings, e); }
}

function navigateWithFilter(cardId, drillDown = {}) {
  const fs = cardOverrides[cardId] || {};
  const p = new URLSearchParams();

  // Base timeline from card
  if (fs.customFrom) p.set('dateFrom', fs.customFrom);
  if (fs.customTo) p.set('dateTo', fs.customTo);

  // Drill down overrides or card defaults
  const types = drillDown.transactionTypes || (fs.txType ? [fs.txType] : []);
  if (types.length === 1) p.set('type', types[0]);
  else if (types.length > 1) {
    // Records page currently only supports single type in URL params easily
    // but we can pass the first one for now or enhance records page later
    p.set('type', types[0]);
  }

  const accts = drillDown.accountIds || fs.accountIds || [];
  if (accts.length === 1) p.set('accountId', accts[0]);

  const cats = drillDown.categoryIds || fs.categoryIds || [];
  if (cats.length === 1) p.set('categoryId', cats[0]);

  if (fs.tags && fs.tags.length === 1) p.set('tag', fs.tags[0]);
  if (fs.currencies && fs.currencies.length === 1) p.set('currency', fs.currencies[0]);
  if (fs.minAmount != null) p.set('minAmount', fs.minAmount);
  if (fs.maxAmount != null) p.set('maxAmount', fs.maxAmount);
  if (fs.queryNote) p.set('search', fs.queryNote);

  p.set('excludeFuture', fs.excludeFuture === false ? '0' : '1');

  location.hash = '#/records?' + p.toString();
}

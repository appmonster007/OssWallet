/* PAGE: Planned Payments */

/* ── helpers ─────────────────────────────────────────────── */

function _ppTypeColor(p) {
  if (p.transactionType === 'INCOME')   return 'var(--income)';
  if (p.transactionType === 'TRANSFER') return 'var(--accent)';
  return 'var(--expense)';
}

function _ppTypeIcon(p) {
  if (p.transactionType === 'INCOME') return `<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"/></svg>`;
  if (p.transactionType === 'TRANSFER') return `<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"/></svg>`;
  return `<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"/></svg>`;
}

function _ppStatusColor(p) {
  if (p.isOverdue)            return 'var(--expense)';
  if (p.status === 'PAUSED')  return 'var(--text-muted)';
  if (p.transactionType === 'INCOME') return 'var(--income)';
  return 'var(--accent)';
}

function _ppRelDate(ts) {
  if (!ts) return '—';
  const diff = ts - Date.now();
  const absDays = Math.round(Math.abs(diff) / 86400000);
  if (diff < 0) return absDays === 0 ? 'Due today' : `${absDays}d overdue`;
  if (absDays === 0) return 'Due today';
  if (absDays === 1) return 'Tomorrow';
  if (absDays <= 7) return `In ${absDays} days`;
  return fmtDate(ts);
}

function _ppAmountSign(p) {
  if (p.transactionType === 'INCOME')   return '+';
  if (p.transactionType === 'TRANSFER') return '';
  return '−';
}

function _ppListItem(p) {
  const typeColor  = _ppTypeColor(p);
  const statusColor = _ppStatusColor(p);
  const relDate    = _ppRelDate(p.nextDueDate);
  const sign       = _ppAmountSign(p);
  const opacity    = p.status === 'PAUSED' ? 'opacity:0.55;' : '';
  const isOverdue  = p.isOverdue;

  return `<div class="pp-item" style="${opacity}" onclick="navigate('#/planned-payments/${esc(p.id)}')">
    <div class="pp-item-icon" style="color:${statusColor};background:${statusColor}18">${_ppTypeIcon(p)}</div>
    <div class="pp-item-body">
      <div class="pp-item-header">
        <span class="pp-item-name">${esc(p.name)}</span>
        <span class="pp-item-amount" style="color:${typeColor}">${sign}${fmt(p.amount, p.currency)}</span>
      </div>
      <div class="pp-item-sub">
        <span>${esc(p.recurrenceDescription || '')}</span>
        ${p.categoryName ? `<span class="pp-item-cat">${esc(p.categoryName)}</span>` : ''}
      </div>
      <div class="pp-item-date-row">
        <span class="pp-item-date" style="color:${isOverdue ? 'var(--expense)' : 'var(--text-muted)'}">${relDate}</span>
        ${p.autoConfirm ? '<span class="pp-item-auto-badge">Auto</span>' : ''}
      </div>
    </div>
    <div class="pp-item-actions">
      ${p.status === 'ACTIVE' ? `
        <button class="pp-menu-btn confirm" onclick="event.stopPropagation();confirmPlannedOccurrence('${esc(p.id)}',${p.nextDueDate})">✓ Confirm</button>
        <button class="pp-menu-btn skip"    onclick="event.stopPropagation();skipPlannedOccurrence('${esc(p.id)}',${p.nextDueDate})">›› Skip</button>
        <button class="pp-menu-btn"         onclick="event.stopPropagation();openEditPlanned('${esc(p.id)}')">Edit</button>
        <button class="pp-menu-btn"         onclick="event.stopPropagation();pausePlanned('${esc(p.id)}')">Pause</button>
      ` : p.status === 'PAUSED' ? `
        <button class="pp-menu-btn confirm" onclick="event.stopPropagation();resumePlanned('${esc(p.id)}')">Resume</button>
        <button class="pp-menu-btn"         onclick="event.stopPropagation();openEditPlanned('${esc(p.id)}')">Edit</button>
      ` : `
        <button class="pp-menu-btn"         onclick="event.stopPropagation();openEditPlanned('${esc(p.id)}')">Edit</button>
      `}
      <button class="pp-menu-btn danger" onclick="event.stopPropagation();deletePlanned('${esc(p.id)}','${esc(p.name)}')">Delete</button>
    </div>
  </div>`;
}

/* ── list page ───────────────────────────────────────────── */

async function renderPlannedPayments() {
  showSpinner();
  try {
    const json = await apiFetch('/api/planned-payments');
    if (!json.success) { showError('Failed to load planned payments.'); return; }
    const all = json.data || [];

    let html = `<div class="page-header"><h2 class="section-title">Planned Payments</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddPlanned()">+ Add Payment</button>
    </div>`;

    if (!all.length) {
      html += emptyState('', 'No planned payments yet.', '+ Add Planned Payment', 'openAddPlanned()');
      setContent(html);
      return;
    }

    const overdue   = all.filter(p => p.isOverdue && p.status === 'ACTIVE');
    const upcoming  = all.filter(p => !p.isOverdue && p.status === 'ACTIVE' && p.nextDueDate <= Date.now() + 7 * 86400000);
    const scheduled = all.filter(p => !p.isOverdue && p.status === 'ACTIVE' && p.nextDueDate > Date.now() + 7 * 86400000);
    const paused    = all.filter(p => p.status === 'PAUSED');

    html += '<div class="pp-list">';
    if (overdue.length) {
      html += `<div class="pp-section-header" style="color:var(--expense)">OVERDUE (${overdue.length})</div>`;
      overdue.forEach(p => { html += _ppListItem(p); });
    }
    if (upcoming.length) {
      html += `<div class="pp-section-header">UPCOMING</div>`;
      upcoming.forEach(p => { html += _ppListItem(p); });
    }
    if (scheduled.length) {
      html += `<div class="pp-section-header">SCHEDULED</div>`;
      scheduled.forEach(p => { html += _ppListItem(p); });
    }
    if (paused.length) {
      html += `<div class="pp-section-header" style="color:var(--text-muted)">PAUSED</div>`;
      paused.forEach(p => { html += _ppListItem(p); });
    }
    html += '</div>';

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading planned payments.'); }
}

/* ── hero card ───────────────────────────────────────────── */

function _ppHeroHtml(p) {
  const typeColor = _ppTypeColor(p);
  const sign      = _ppAmountSign(p);
  const typeLabel = p.transactionType === 'INCOME' ? 'Income' : p.transactionType === 'TRANSFER' ? 'Transfer' : 'Expense';

  const repeatIcon = `<svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z"/></svg>`;

  const relDate   = _ppRelDate(p.nextDueDate);
  const dateColor = p.isOverdue ? 'var(--expense)' : 'var(--accent)';

  return `<div class="pp-hero" style="--pp-accent:${typeColor}">
    <div class="pp-hero-title-row">
      <div class="pp-hero-icon-wrap" style="background:${typeColor}22;color:${typeColor}">${repeatIcon}</div>
      <div>
        <div class="pp-hero-card-title">${esc(p.name)}</div>
        <div class="pp-hero-card-sub">${typeLabel} &bull; ${esc(p.recurrenceDescription || 'One Time')}</div>
      </div>
    </div>
    <div class="pp-hero-amount" style="color:${typeColor}">${sign}${fmt(p.amount, p.currency)}</div>
    <div class="pp-hero-due" style="color:${dateColor}">Next due: ${relDate} &bull; ${fmtDate(p.nextDueDate)}</div>
    ${p.status === 'PAUSED' ? '<div class="pp-hero-paused-banner">⏸ This payment is paused</div>' : ''}
  </div>`;
}

/* ── details card ─────────────────────────────────────────── */

function _ppDetailsHtml(p) {
  const acct = p.accountId ? (cachedAccounts.find(a => String(a.id) === String(p.accountId))?.name || p.accountName || p.accountId) : '—';
  const cat  = p.categoryId ? (cachedCategories.find(c => String(c.id) === String(p.categoryId))?.name || p.categoryName || '—') : '—';

  const calIcon     = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z"/></svg>`;
  const acctIcon    = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M4 10v7h3v-7H4zm6 0v7h3v-7h-3zm-8 9h19v3H2v-3zm14-9v7h3v-7h-3zM11.5 1L2 6v2h19V6l-9.5-5z"/></svg>`;
  const catIcon     = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 2l-5.5 9h11L12 2zm0 3.84L13.93 9h-3.87L12 5.84zM17.5 13c-2.49 0-4.5 2.01-4.5 4.5S15.01 22 17.5 22s4.5-2.01 4.5-4.5S19.99 13 17.5 13zm0 7c-1.38 0-2.5-1.12-2.5-2.5S16.12 15 17.5 15s2.5 1.12 2.5 2.5S18.88 20 17.5 20zM3 21.5h8v-8H3v8zm2-6h4v4H5v-4z"/></svg>`;
  const personIcon  = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`;
  const noteIcon    = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M14 17H4v2h10v-2zm6-8H4v2h16V9zM4 15h16v-2H4v2zM4 5v2h16V5H4z"/></svg>`;
  const tagIcon     = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.37.86.58 1.41.58s1.05-.21 1.41-.58l7-7c.37-.36.59-.86.59-1.42 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"/></svg>`;
  const autoIcon    = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 8l-4 4h3c0 3.31-2.69 6-6 6-1.01 0-1.96-.25-2.8-.7l-1.46 1.46C8.97 19.54 10.43 20 12 20c4.42 0 8-3.58 8-8h3l-4-4zM6 12c0-3.31 2.69-6 6-6 1.01 0 1.96.25 2.8.7l1.46-1.46C15.03 4.46 13.57 4 12 4c-4.42 0-8 3.58-8 8H1l4 4 4-4H6z"/></svg>`;
  const methodIcon  = `<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M20 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z"/></svg>`;

  const rows = [
    { icon: calIcon,    key: 'SCHEDULE',   val: esc(p.recurrenceDescription || 'One Time') },
    { icon: acctIcon,   key: 'ACCOUNT',    val: esc(acct) },
    { icon: catIcon,    key: 'CATEGORY',   val: esc(cat) },
    p.payee ? { icon: personIcon, key: 'PAYEE', val: esc(p.payee) } : null,
    p.note  ? { icon: noteIcon,   key: 'NOTE',  val: esc(p.note) } : null,
    (p.tags && p.tags.length) ? { icon: tagIcon, key: 'TAGS', val: p.tags.map(t => `<span class="pp-tag-chip">${esc(t)}</span>`).join('') } : null,
    p.autoConfirm ? { icon: autoIcon, key: 'AUTOMATION', val: '<span style="color:var(--accent)">Auto-confirm enabled</span>' } : null,
    p.paymentMethod ? { icon: methodIcon, key: 'METHOD', val: esc(prettyEnum(p.paymentMethod)) } : null,
  ].filter(Boolean);

  return `<div class="pp-scope-card">
    <div class="pp-scope-header">Details</div>
    ${rows.map(r => `<div class="pp-scope-row">
      <span class="pp-scope-icon">${r.icon}</span>
      <span class="pp-scope-key">${r.key}</span>
      <span class="pp-scope-val">${r.val}</span>
    </div>`).join('')}
  </div>`;
}

/* ── upcoming occurrences ─────────────────────────────────── */

function _ppUpcomingHtml(p) {
  // Generate next 6 occurrences client-side
  const occurrences = _generateOccurrences(p, 6);
  if (!occurrences.length) return '';

  const now = Date.now();
  return `<div class="pp-scope-card">
    <div class="pp-scope-header">UPCOMING (${occurrences.length})</div>
    ${occurrences.map((date, i) => {
      const isOverdue = date < now;
      const relDate   = _ppRelDate(date);
      const dateColor = isOverdue ? 'var(--expense)' : 'var(--accent)';
      return `<div class="pp-occ-row">
        <div class="pp-occ-left">
          <div class="pp-occ-date" style="color:${dateColor}">${fmtDate(date)}</div>
          <div class="pp-occ-rel" style="color:${dateColor}">${relDate}</div>
        </div>
        <div class="pp-occ-amount">${_ppAmountSign(p)}${fmt(p.amount, p.currency)}</div>
        <div class="pp-occ-btns">
          <button class="pp-occ-btn skip" title="Skip" onclick="skipPlannedOccurrence('${esc(p.id)}',${date})">›</button>
          <button class="pp-occ-btn confirm" title="Confirm" onclick="confirmPlannedOccurrence('${esc(p.id)}',${date})">✓</button>
        </div>
      </div>`;
    }).join('')}
  </div>`;
}

function _generateOccurrences(p, count) {
  if (!p.nextDueDate) return [];
  const dates = [p.nextDueDate];
  const type = p.recurrenceType || 'MONTHLY';
  if (type === 'ONE_TIME') return dates;
  let last = p.nextDueDate;
  for (let i = 1; i < count; i++) {
    const d = new Date(last);
    if      (type === 'DAILY')   d.setDate(d.getDate() + 1);
    else if (type === 'WEEKLY')  d.setDate(d.getDate() + 7);
    else if (type === 'MONTHLY') d.setMonth(d.getMonth() + 1);
    else if (type === 'YEARLY')  d.setFullYear(d.getFullYear() + 1);
    last = d.getTime();
    dates.push(last);
  }
  return dates;
}

/* ── history tab ─────────────────────────────────────────── */

function _ppHistoryHtml(occurrences) {
  if (!occurrences.length) return '<div class="pp-history-empty">No history yet.</div>';
  return `<div class="pp-history-list">
    ${occurrences.map(occ => {
      const statusColor = occ.status === 'CONFIRMED' ? 'var(--income)' : occ.status === 'SKIPPED' ? 'var(--text-muted)' : 'var(--accent)';
      const statusLabel = occ.status === 'CONFIRMED' ? '✓ Confirmed' : occ.status === 'SKIPPED' ? '›› Skipped' : 'Pending';
      return `<div class="pp-history-row">
        <div class="pp-history-date">${fmtDate(occ.scheduledDate)}</div>
        <div class="pp-history-status" style="color:${statusColor}">${statusLabel}</div>
      </div>`;
    }).join('')}
  </div>`;
}

/* ── tab switcher ────────────────────────────────────────── */

window.ppDetailTab = function (tab) {
  document.querySelectorAll('.pp-detail-tab').forEach(el => el.classList.toggle('active', el.dataset.tab === tab));
  document.querySelectorAll('.pp-detail-pane').forEach(el => el.classList.toggle('active', el.dataset.pane === tab));
};

/* ── detail page ─────────────────────────────────────────── */

async function renderPlannedPaymentDetail(id) {
  if (!id) { navigate('#/planned-payments'); return; }
  showSpinner();
  try {
    const [ppRes, occRes] = await Promise.all([
      apiFetch(`/api/planned-payments/${encodeURIComponent(id)}`),
      apiFetch(`/api/planned-payments/${encodeURIComponent(id)}/occurrences`).catch(() => ({ success: true, data: [] }))
    ]);
    if (!ppRes.success) { showError('Failed to load planned payment.'); return; }
    const p = ppRes.data || {};
    const occurrences = (occRes.success ? (occRes.data || []) : []).filter(o => o.scheduledDate < Date.now()).sort((a, b) => b.scheduledDate - a.scheduledDate);

    const typeColor = _ppTypeColor(p);

    let html = `<div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <a class="btn btn-ghost btn-sm" href="#/planned-payments">← Back</a>
        <h2 class="section-title" style="margin:0">${esc(p.name)}</h2>
      </div>
      <div style="display:flex;gap:8px">
        <button class="btn btn-ghost btn-sm" onclick="openEditPlanned('${esc(p.id)}')">Edit</button>
        ${p.status === 'ACTIVE' ? `<button class="btn btn-primary btn-sm" onclick="confirmPlannedOccurrence('${esc(p.id)}',${p.nextDueDate})">✓ Confirm</button>` : ''}
      </div>
    </div>

    <div class="pp-detail-pill-nav">
      <div class="pp-detail-tabs">
        <button class="pp-detail-tab active" data-tab="overview" onclick="ppDetailTab('overview')">Overview</button>
        <button class="pp-detail-tab" data-tab="history" onclick="ppDetailTab('history')">History</button>
      </div>
    </div>

    <div class="pp-detail-pane active" data-pane="overview">
      ${_ppHeroHtml(p)}
      ${_ppDetailsHtml(p)}
      ${_ppUpcomingHtml(p)}
    </div>

    <div class="pp-detail-pane" data-pane="history">
      <div class="pp-history-header">Past Occurrences</div>
      ${_ppHistoryHtml(occurrences)}
    </div>`;

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading planned payment.'); }
}

/* ── confirm / skip ──────────────────────────────────────── */
// confirmPlannedOccurrence and skipPlannedOccurrence are defined in transactions.js

window.pausePlanned = async function (id) {
  try {
    const res = await apiWrite('PUT', `/api/planned-payments/${encodeURIComponent(id)}`, { status: 'PAUSED' });
    if (res.success) { renderPlannedPayments(); showToast('Payment paused.'); }
    else { showToast('Failed to pause.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error pausing.', 'error'); }
};

window.resumePlanned = async function (id) {
  try {
    const res = await apiWrite('PUT', `/api/planned-payments/${encodeURIComponent(id)}`, { status: 'ACTIVE' });
    if (res.success) { renderPlannedPayments(); showToast('Payment resumed.'); }
    else { showToast('Failed to resume.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error resuming.', 'error'); }
};

/* ── add / edit modals ───────────────────────────────────── */

window.openAddPlanned = function () {
  const body = `
    ${formField('Payment Name', '<input type="text" id="pp-name" placeholder="e.g. Netflix">')}
    ${formRow(
      formField('Amount', `<div class="input-group"><span class="input-group-addon" id="pp-amount-pfx">${esc(cachedSettings.baseCurrency||'USD')}</span><input type="number" id="pp-amount" placeholder="0.00" min="0" step="0.01"></div>`),
      formField('Currency', `<input type="text" id="pp-currency" value="${esc(cachedSettings.baseCurrency||'USD')}" maxlength="5" oninput="const p=document.getElementById('pp-amount-pfx');if(p)p.textContent=this.value.toUpperCase()||'—'">`)
    )}
    ${formRow(
      formField('Type', buildSelect('pp-type',[{value:'EXPENSE',label:'Expense'},{value:'INCOME',label:'Income'},{value:'TRANSFER',label:'Transfer'}],'EXPENSE','— Select —',false,"document.getElementById('pp-to-account-row').style.display=this.value==='TRANSFER'?'':'none'")),
      formField('Account', buildSelect('pp-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),'','— Select account —',true))
    )}
    <div id="pp-to-account-row" style="display:none">
      ${formField('To Account', buildSelect('pp-to-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),'','— Select destination —',true))}
    </div>
    ${formField('Category', buildSelect('pp-category',[{value:'',label:'— None —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),'','— None —',true))}
    ${formRow(
      formField('Recurrence', buildSelect('pp-recur',[{value:'ONE_TIME',label:'One Time'},{value:'DAILY',label:'Daily'},{value:'WEEKLY',label:'Weekly'},{value:'MONTHLY',label:'Monthly'},{value:'YEARLY',label:'Yearly'}],'MONTHLY','— Select —',false)),
      formField('Interval', '<input type="number" id="pp-interval" value="1" min="1" step="1">')
    )}
    ${formRow(formField('Start Date', '<input type="date" id="pp-start">'),
              formField('Payment Method', buildSelect('pp-method',[{value:'',label:'— None —'},{value:'CASH',label:'Cash'},{value:'CARD',label:'Card'},{value:'BANK_TRANSFER',label:'Bank Transfer'},{value:'DIGITAL_WALLET',label:'Digital Wallet'},{value:'OTHER',label:'Other'}],'','— None —',false))
    )}
    ${formRow(
      formField('Payee', `<input type="text" id="pp-payee" placeholder="Optional payee">`),
      formField('Location', `<input type="text" id="pp-location" placeholder="Optional location">`)
    )}
    ${formField('Tags', `<div id="pp-tags-container"></div>`)}
    ${formField('Note (optional)', '<input type="text" id="pp-note" placeholder="Optional note…">')}
    <div class="form-field"><label>Auto-Confirm</label>
      <div class="toggle-wrap"><input type="checkbox" id="pp-auto"><span style="font-size:0.88rem">Automatically confirm when due</span></div>
    </div>`;
  openModal('Add Planned Payment', body, submitAddPlanned, 'Save Payment');
  modalConfBtn.className = 'btn btn-primary';
  initTagChipInput('pp-tags', cachedTags, []);
  const startEl = document.getElementById('pp-start');
  if (startEl) startEl.value = new Date().toISOString().slice(0,10);
};

window.submitAddPlanned = async function () {
  const get = id => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  const name    = get('pp-name');
  const amount  = parseFloat(get('pp-amount') || '0');
  const currency= get('pp-currency');
  const type    = get('pp-type');
  const account = get('pp-account');
  const cat     = get('pp-category');
  const recur   = get('pp-recur');
  const interval= parseInt(get('pp-interval') || '1', 10);
  const start   = get('pp-start');
  const method  = get('pp-method');
  const note    = get('pp-note');
  const auto    = document.getElementById('pp-auto')?.checked || false;
  const payee   = get('pp-payee');
  const loc     = get('pp-location');
  const tags    = get('pp-tags') ? get('pp-tags').split(',').map(t=>t.trim()).filter(Boolean) : [];

  clearFieldErrors(modalBody);
  if (!name)                      { showFieldError('pp-name', 'Name is required.'); return; }
  if (isNaN(amount) || amount <= 0) { showFieldError('pp-amount', 'Enter a valid amount.'); return; }
  if (!account) { showFieldError('pp-account', 'Account is required.'); return; }
  await withLoading(modalConfBtn, async () => {
    const body = {
      name, amount, currency, transactionType: type,
      accountId: account,
      categoryId: cat || undefined,
      recurrenceType: recur,
      recurrenceInterval: recur === 'ONE_TIME' ? 1 : interval,
      startDate: start || undefined,
      autoConfirm: auto,
      paymentMethod: method || undefined,
      note: note || undefined,
      payee: payee || undefined,
      location: loc || undefined,
      tags
    };
    if (type === 'TRANSFER') body.targetAccountId = get('pp-to-account') || undefined;
    const res = await apiWrite('POST', '/api/planned-payments', body);
    if (res.success) { closeModal(); renderPlannedPayments(); showToast('Planned payment created!'); }
    else { showToast('Failed to create: ' + (res.error || ''), 'error'); }
  });
};

window.openEditPlanned = async function (id) {
  const resp = await apiFetch(`/api/planned-payments/${encodeURIComponent(id)}`);
  if (!resp.success) { showToast('Could not load planned payment.', 'error'); return; }
  const p = resp.data;
  const body = `
    ${formField('Payment Name', `<input type="text" id="pp-name" value="${esc(p.name)}">`)}
    ${formRow(
      formField('Amount', `<div class="input-group"><span class="input-group-addon" id="pp-amount-pfx">${esc(p.currency||cachedSettings.baseCurrency||'USD')}</span><input type="number" id="pp-amount" value="${esc(p.amount||0)}" min="0" step="0.01"></div>`),
      formField('Currency', `<input type="text" id="pp-currency" value="${esc(p.currency||cachedSettings.baseCurrency||'USD')}" maxlength="5" oninput="const pfx=document.getElementById('pp-amount-pfx');if(pfx)pfx.textContent=this.value.toUpperCase()||'—'">`)
    )}
    ${formRow(
      formField('Type', buildSelect('pp-type',[{value:'EXPENSE',label:'Expense'},{value:'INCOME',label:'Income'},{value:'TRANSFER',label:'Transfer'}],p.transactionType||'EXPENSE','— Select —',false,"document.getElementById('pp-to-account-row').style.display=this.value==='TRANSFER'?'':'none'")),
      formField('Account', buildSelect('pp-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),p.accountId||'','— Select account —',true))
    )}
    <div id="pp-to-account-row" style="${p.transactionType==='TRANSFER'?'':'display:none'}">
      ${formField('To Account', buildSelect('pp-to-account',cachedAccounts.map(a=>({value:a.id,label:`${a.name} (${a.currency})`})),p.targetAccountId||'','— Select destination —',true))}
    </div>
    ${formField('Category', buildSelect('pp-category',[{value:'',label:'— None —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),p.categoryId?String(p.categoryId):'','— None —',true))}
    ${formRow(
      formField('Recurrence', buildSelect('pp-recur',[{value:'ONE_TIME',label:'One Time'},{value:'DAILY',label:'Daily'},{value:'WEEKLY',label:'Weekly'},{value:'MONTHLY',label:'Monthly'},{value:'YEARLY',label:'Yearly'}],p.recurrenceType||'MONTHLY','— Select —',false)),
      formField('Interval', `<input type="number" id="pp-interval" value="${esc(p.recurrenceInterval||1)}" min="1" step="1">`)
    )}
    ${formRow(
      formField('Payee', `<input type="text" id="pp-payee" value="${esc(p.payee||'')}" placeholder="Optional payee">`),
      formField('Location', `<input type="text" id="pp-location" value="${esc(p.location||'')}" placeholder="Optional location">`)
    )}
    ${formField('Tags', `<div id="pp-tags-container"></div>`)}
    ${formField('Note (optional)', `<input type="text" id="pp-note" value="${esc(p.note||'')}" placeholder="Optional note…">`)}
    <div class="form-field"><label>Auto-Confirm</label>
      <div class="toggle-wrap"><input type="checkbox" id="pp-auto" ${p.autoConfirm?'checked':''}><span style="font-size:0.88rem">Automatically confirm when due</span></div>
    </div>`;
  openModal(`Edit — ${esc(p.name)}`, body, async () => {
    const get = i => { const el = document.getElementById(i); return el ? el.value.trim() : ''; };
    clearFieldErrors(modalBody);
    const name   = get('pp-name');
    const amount = parseFloat(get('pp-amount') || '0');
    const currency = get('pp-currency');
    const type   = get('pp-type');
    const account= get('pp-account');
    const cat    = get('pp-category');
    const recur  = get('pp-recur');
    const interval = parseInt(get('pp-interval') || '1', 10);
    const auto   = document.getElementById('pp-auto')?.checked || false;
    const payee  = get('pp-payee');
    const loc    = get('pp-location');
    const tags   = get('pp-tags') ? get('pp-tags').split(',').map(t=>t.trim()).filter(Boolean) : [];

    if (!name)                      { showFieldError('pp-name', 'Name is required.'); return; }
    if (isNaN(amount) || amount <= 0) { showFieldError('pp-amount', 'Enter a valid amount.'); return; }
    await withLoading(modalConfBtn, async () => {
      const payload = {
        name, amount, currency, transactionType: type,
        accountId: account || undefined,
        categoryId: cat || undefined,
        recurrenceType: recur,
        recurrenceInterval: interval,
        autoConfirm: auto,
        payee: payee || undefined,
        location: loc || undefined,
        tags
      };
      if (type === 'TRANSFER') payload.targetAccountId = get('pp-to-account') || undefined;
      const res = await apiWrite('PUT', `/api/planned-payments/${encodeURIComponent(id)}`, payload);
      if (res.success) { closeModal(); renderPlannedPayments(); showToast('Planned payment updated!'); }
      else { showToast('Failed to update: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
  initTagChipInput('pp-tags', cachedTags, p.tags || []);
  if (p.accountId)       { const el = document.getElementById('pp-account');     if (el) el.value = p.accountId; }
  if (p.categoryId)      { const el = document.getElementById('pp-category');    if (el) el.value = String(p.categoryId); }
  if (p.targetAccountId) { const el = document.getElementById('pp-to-account'); if (el) el.value = p.targetAccountId; }
};

window.deletePlanned = function (id, name) {
  confirmDelete(`Delete planned payment "${name}"?`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/planned-payments/${encodeURIComponent(id)}`);
      closeModal();
      if (res.success) { renderPlannedPayments(); showToast('Planned payment deleted.'); }
      else { showToast('Failed to delete planned payment.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting.', 'error'); }
  });
};

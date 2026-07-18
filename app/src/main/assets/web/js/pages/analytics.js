/* PAGE: Analytics (Intelligence) */

let analyticsActiveTab = 'Status';
let lastAnalyticsResponse = null;

async function renderAnalytics() {
  showSpinner();
  try {
    const json = await apiFetch('/api/analytics');
    if (!json.success) { showError('Failed to load analytics.'); return; }

    lastAnalyticsResponse = json.data;
    const cards = json.data.cards || [];

    // Cache card filter overrides
    cards.forEach(card => {
      const fs = card.filterSummary;
      if (fs && !cardOverrides[card.cardId]) {
        cardOverrides[card.cardId] = {
          timelineType: fs.timelineType || 'ALL_TIME',
          customFrom: fs.dateFrom ? new Date(fs.dateFrom).toISOString().slice(0,10) : null,
          customTo: fs.dateTo ? new Date(fs.dateTo).toISOString().slice(0,10) : null,
          accountIds: fs.accountIds || [],
          categoryIds: fs.categoryIds || [],
          txType: (fs.transactionTypes || [])[0] || '',
          currencies: fs.currencies || [],
          statuses: fs.statuses || [],
          paymentMethods: fs.paymentMethods || [],
          tags: fs.tags || [],
          queryNote: fs.queryNote || '',
          queryPayee: fs.queryPayee || '',
          minAmount: fs.minAmount ?? null,
          maxAmount: fs.maxAmount ?? null,
          excludeFuture: fs.excludeFuture !== false,
        };
      }
    });

    renderAnalyticsContent(json.data);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading intelligence.'); }
}

function renderAnalyticsContent(data) {
  const cards = data.cards || [];
  const commonFilterEnabled = data.commonFilterEnabled;
  const tabs = ['Status', 'Wallet', 'Track', 'Charts', 'Graphs'];

  let html = `
    <div class="page-header" style="flex-direction:column;align-items:flex-start;gap:12px">
      <div style="display:flex;justify-content:space-between;width:100%;align-items:center">
        <h2 class="section-title">Intelligence</h2>
        <div style="display:flex;gap:8px">
          <button class="btn btn-ghost btn-sm" onclick="toggleCommonFilter(${!commonFilterEnabled})">
            ${commonFilterEnabled ? 'Individual Filters' : 'Use Common Filter'}
          </button>
          <button class="btn btn-ghost btn-sm" onclick="openManageAnalyticsCards()">Settings</button>
        </div>
      </div>
      <div class="atelier-tabs">
        ${tabs.map(tab => `<div class="atelier-tab${analyticsActiveTab === tab ? ' active' : ''}" onclick="switchAnalyticsTab('${tab}')">${tab}</div>`).join('')}
      </div>
    </div>
  `;

  updateAnalyticsFilterPill(data);

  const filteredCards = cards.filter(c => getTabForCardType(c.cardType) === analyticsActiveTab);

  if (!filteredCards.length) {
    html += `<div style="text-align:center;padding:60px 20px;color:var(--text-muted)">
      <p>No <strong>${analyticsActiveTab.toLowerCase()}</strong> widgets active in Intelligence.</p>
    </div>`;
  } else {
    filteredCards.forEach(card => {
      try {
        const d = card.data || {};
        const payloadType = d.type ? d.type.split('.').pop() : '';
        html += dispatchCardRender(card, d, payloadType);
      } catch (e) {
        console.error('Error rendering analytics card', card, e);
        html += `<div class="card"><div class="alert alert-error">Error rendering card "${esc(card.title)}": ${esc(e.message)}</div></div>`;
      }
    });
  }

  setContent(html);
}

function getTabForCardType(type) {
  if (['NET_WORTH', 'CASH_FLOW', 'RECENT_ACTIVITIES'].includes(type)) return 'Status';
  if (['ACCOUNTS', 'CURRENCY_BALANCES', 'ACCOUNT_HERO'].includes(type)) return 'Wallet';
  if (['BUDGETS', 'PLANNED_PAYMENTS', 'LOANS'].includes(type)) return 'Track';
  if (['CATEGORY_PIE_CHART', 'CATEGORY_TREEMAP', 'TAG_UPSET_PLOT'].includes(type)) return 'Charts';
  if (['CASHFLOW_COMPARISON', 'INCOME_EXPENSE_BAR', 'CATEGORY_SPENDING_TREND'].includes(type)) return 'Graphs';
  return 'Status';
}

window.switchAnalyticsTab = function(tab) {
  analyticsActiveTab = tab;
  // Render from cache — never re-fetch on tab switch
  if (lastAnalyticsResponse) {
    renderAnalyticsContent(lastAnalyticsResponse);
  } else {
    renderAnalytics();
  }
};

window.toggleCommonFilter = async function(enabled) {
  showSpinner();
  try {
    const res = await apiFetch('/api/analytics/common-filter', {
      method: 'POST',
      body: JSON.stringify(enabled)
    });
    if (res.success) renderAnalytics();
    else showError('Failed to update filter mode.');
  } catch (e) { showError('Error.'); }
};

const TL_LABELS = {ALL_TIME:'All Time',TODAY:'Today',WEEK:'This Week',MONTH:'This Month',QUARTER:'This Quarter',YEAR:'This Year',HALF_YEAR:'Half Year',LAST_7_DAYS:'Last 7 Days',LAST_30_DAYS:'Last 30 Days',LAST_6_MONTHS:'Last 6 Mo',LAST_1_YEAR:'Last Year',CUSTOM:'Custom Range'};

function updateAnalyticsFilterPill(data) {
  const pill  = document.getElementById('analytics-filter-pill');
  const label = document.getElementById('analytics-filter-pill-label');
  if (!pill) return;
  if (data.commonFilterEnabled) {
    const fs = data.screenFilter || {};
    const tl = fs.timelineType || 'ALL_TIME';
    label.textContent = TL_LABELS[tl] || tl;
    pill.classList.add('visible');
  } else {
    pill.classList.remove('visible');
  }
}

window.openCommonFilterSheet = function() {
  const existing = lastAnalyticsResponse?.screenFilter || {};
  const body = buildFilterForm(existing);

  openFilterSheet('Common Filter', body, async () => {
    const payload = collectFilterPayload();
    if (!payload) return;
    const res = await apiWrite('POST', '/api/analytics/filter', payload);
    if (res.success) { renderAnalytics(); showToast('Common filter updated'); }
    else { showToast('Failed to update filter: ' + (res.error || ''), 'error'); }
  }, null);

  // Hide name field — not needed for common filter
  requestAnimationFrame(() => {
    const nameField = document.getElementById('fl-name')?.closest('.form-field');
    if (nameField) nameField.style.display = 'none';
  });
};

window.openManageAnalyticsCards = async function() {
  const cards = lastAnalyticsResponse?.cards || [];
  const activeTabCards = cards.filter(c => getTabForCardType(c.cardType) === analyticsActiveTab);
  const cardIds = activeTabCards.map(c => c.cardId);

  let body = `<div style="font-size:0.78rem;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:.05em;margin-bottom:8px">Active in ${analyticsActiveTab}</div>`;

  if (activeTabCards.length) {
    activeTabCards.forEach((c, idx) => {
      body += `<div class="manage-card-row">
        <div style="display:flex;gap:3px">
          <button class="btn btn-ghost btn-sm" onclick="reorderAnalyticsCard('${analyticsActiveTab}',${idx},-1)" ${idx===0?'disabled':''}>↑</button>
          <button class="btn btn-ghost btn-sm" onclick="reorderAnalyticsCard('${analyticsActiveTab}',${idx},1)" ${idx===activeTabCards.length-1?'disabled':''}>↓</button>
        </div>
        <div class="manage-card-name">${esc(c.title||c.cardType)}</div>
        <button class="btn btn-sm" style="background:var(--danger);color:#fff;flex-shrink:0" onclick="removeDashboardCard('${esc(c.cardId)}')">Remove</button>
      </div>`;
    });
  } else {
    body += '<p class="empty-state">No cards in this tab.</p>';
  }

  openModal(`Manage ${analyticsActiveTab} Cards`, body, null, 'Done');
  modalConfBtn.onclick = closeModal;
};

window.reorderAnalyticsCard = async function(tabName, idx, dir) {
  const cards = lastAnalyticsResponse?.cards || [];
  const activeTabCards = cards.filter(c => getTabForCardType(c.cardType) === tabName);

  const newTabCards = [...activeTabCards];
  const swap = idx + dir;
  if (swap < 0 || swap >= newTabCards.length) return;
  [newTabCards[idx], newTabCards[swap]] = [newTabCards[swap], newTabCards[idx]];

  try {
    const res = await apiWrite('PUT', `/api/analytics/reorder?tab=${encodeURIComponent(tabName)}`, { cardIds: newTabCards.map(c => c.cardId) });
    if (res.success) { closeModal(); renderAnalytics(); }
    else { showToast('Failed to reorder: ' + (res.error || ''), 'error'); }
  } catch (e) { showToast('Error reordering.', 'error'); }
};

/* PAGE: Dashboard */

async function renderDashboard() {
  showSpinner();
  try {
    const json = await apiFetch('/api/dashboard');
    if (!json.success) { showError('Failed to load dashboard.'); return; }
    window.lastDashboardData = json.data;
    const cards = (Array.isArray(json.data) ? json.data : [])
      .filter(c => c.isEnabled !== false)
      .sort((a, b) => (a.order || 0) - (b.order || 0));

    cards.forEach(card => {
      const fs = card.filterSummary;
      if (fs && !cardOverrides[card.cardId]) {
        cardOverrides[card.cardId] = {
          timelineType:   fs.timelineType   || 'ALL_TIME',
          customFrom: fs.dateFrom ? new Date(fs.dateFrom).toISOString().slice(0,10) : null,
          customTo:   fs.dateTo   ? new Date(fs.dateTo).toISOString().slice(0,10)   : null,
          accountIds:     fs.accountIds      || [],
          categoryIds:    fs.categoryIds     || [],
          txType:         (fs.transactionTypes||[])[0] || '',
          currencies:     fs.currencies      || [],
          statuses:       fs.statuses        || [],
          paymentMethods: fs.paymentMethods  || [],
          tags:           fs.tags            || [],
          queryNote:      fs.queryNote       || '',
          queryPayee:     fs.queryPayee      || '',
          minAmount:      fs.minAmount       ?? null,
          maxAmount:      fs.maxAmount       ?? null,
          excludeFuture:  fs.excludeFuture   !== false,
        };
      }
    });

    const pinnedCard = cards.find(c => c.cardType === 'ACCOUNTS');
    const regularCards = pinnedCard ? cards.filter(c => c.cardId !== pinnedCard.cardId) : cards;

    let html = `<div class="page-header">
      <h2 class="section-title">Dashboard</h2>
      <button class="btn btn-ghost btn-sm" onclick="openManageCards()">⚙️ Manage Cards</button>
    </div>`;

    if (pinnedCard) {
      try {
        const d = pinnedCard.data || {};
        const payloadType = d.type ? d.type.split('.').pop() : 'Accounts';
        html += pinnedAccountsWrap(pinnedCard, dispatchCardBody(pinnedCard, d, payloadType), payloadType);
      } catch (e) {
        console.error('Error rendering pinned card', e);
      }
    }

    if (!regularCards.length && !pinnedCard) {
      html += '<div class="alert alert-warn">No dashboard cards are enabled. Click <strong>Manage Cards</strong> to add some.</div>';
    } else {
      regularCards.forEach(card => {
        try {
          const d = card.data || {};
          const payloadType = d.type ? d.type.split('.').pop() : '';
          html += dispatchCardRender(card, d, payloadType);
        } catch (e) {
          console.error('Error rendering card', card, e);
          html += `<div class="card" style="border:1px solid var(--danger)"><div class="alert alert-error">Error rendering card "${esc(card.title)}": ${esc(e.message)}</div></div>`;
        }
      });
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading dashboard.'); }
}

async function refreshCardContent(cardId, payloadType, cardType) {
  const el = document.getElementById('card-body-' + cardId);
  if (!el) return;
  el.innerHTML = '<div class="loading-wrap"><div class="spinner" style="width:20px;height:20px;border-width:2px;margin:20px auto"></div></div>';
  try {
    const resp = await apiFetch('/api/dashboard');
    const cards = resp.success ? (resp.data || []) : [];
    const card = cards.find(c => c.cardId === cardId);
    if (!card) { el.innerHTML = '<div class="alert alert-warn" style="font-size:0.85rem">Card not found in dashboard.</div>'; return; }
    const d  = card.data;
    const pt = d.type ? d.type.split('.').pop() : payloadType;
    el.innerHTML = dispatchCardBody(card, d, pt);
    const badgeEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-header .filter-badge`);
    const newBadge = filterBadge(card.filterSummary);
    if (badgeEl) { badgeEl.outerHTML = newBadge || ''; }
    else if (newBadge) { const titleEl = document.querySelector(`[data-card-id="${cardId}"] .dash-card-title`); if (titleEl) titleEl.insertAdjacentHTML('afterend', newBadge); }
  } catch (e) {
    if (e.message !== 'Session expired') el.innerHTML = '<div class="alert alert-error">Error refreshing card.</div>';
  }
}

/* Card body renderers moved to individual component files in js/components/cards/ */

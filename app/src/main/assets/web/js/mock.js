/* Mock API layer — active only when ?mock=true in the URL */
/* Overrides apiFetch and apiWrite with fixture-based responses.  */
/* Run with: npx serve app/src/main/assets  →  http://localhost:3000/web/?mock=true */

(function () {
  if (new URLSearchParams(location.search).get('mock') !== 'true') return;

  const DATA = window.MOCK_DATA || {};

  /* ── path normalisation ─────────────────────────────────────────────────── */
  function normPath(path) {
    // strip query string
    return path.split('?')[0].replace(/\/+$/, '');
  }

  function findFixture(path) {
    const key = normPath(path);
    if (DATA[key]) return DATA[key];

    // try prefix with upcoming/overdue stripped
    const upcomingKey = key.replace(/\/upcoming$/, '/upcoming');
    const overdueKey  = key.replace(/\/overdue$/,  '/overdue');
    if (DATA[upcomingKey]) return DATA[upcomingKey];
    if (DATA[overdueKey])  return DATA[overdueKey];

    // wildcard: match /api/foo/ANYTHING against registered /api/foo/:id keys
    const parts  = key.split('/');
    const parent = parts.slice(0, -1).join('/');
    const id     = parts[parts.length - 1];

    // look for /parent/:id patterns (check if any registered key starts with parent/)
    for (const k of Object.keys(DATA)) {
      if (k.startsWith(parent + '/') && k.split('/').length === parts.length) {
        return DATA[k]; // first match (not exact but close enough for basic mocking)
      }
    }
    return null;
  }

  /* ── simulated delay (realistic feel) ──────────────────────────────────── */
  const delay = (ms) => new Promise(r => setTimeout(r, ms));
  const jitter = () => 40 + Math.random() * 80; // 40–120 ms

  /* ── dynamic transaction filtering ─────────────────────────────────────── */
  function handleTransactions(path) {
    const qStr = path.split('?')[1] || '';
    const q    = new URLSearchParams(qStr);
    const split = s => (s || '').split(',').map(x => x.trim()).filter(Boolean);

    const types    = split(q.get('types')    || q.get('type'));
    const accIds   = split(q.get('accountIds') || q.get('accountId'));
    const catIds   = split(q.get('categoryIds') || q.get('categoryId')).map(Number);
    const statuses = split(q.get('statuses')  || q.get('status'));
    const methods  = split(q.get('paymentMethods') || q.get('paymentMethod'));
    const tagNames = split(q.get('tagNames')  || q.get('tag'));
    const dateFrom = q.get('dateFrom') ? new Date(q.get('dateFrom')).getTime() : null;
    const dateTo   = q.get('dateTo')   ? new Date(q.get('dateTo')).getTime()   : null;
    const minAmt   = q.get('minAmount') != null && q.get('minAmount') !== '' ? parseFloat(q.get('minAmount')) : null;
    const maxAmt   = q.get('maxAmount') != null && q.get('maxAmount') !== '' ? parseFloat(q.get('maxAmount')) : null;
    const qPayee   = (q.get('queryPayee') || '').toLowerCase().trim();
    const page     = parseInt(q.get('page')  || '0', 10);
    const limit    = parseInt(q.get('limit') || '25', 10);

    let txs = (window.MOCK_TRANSACTIONS || []).slice();
    if (types.length)    txs = txs.filter(t => types.includes(t.type));
    if (accIds.length)   txs = txs.filter(t => accIds.includes(t.accountId));
    if (catIds.length)   txs = txs.filter(t => catIds.includes(t.categoryId));
    if (statuses.length) txs = txs.filter(t => statuses.includes(t.status));
    if (methods.length)  txs = txs.filter(t => methods.includes(t.paymentMethod));
    if (tagNames.length) txs = txs.filter(t => t.tags && tagNames.some(tag => t.tags.includes(tag)));
    if (dateFrom != null)txs = txs.filter(t => t.date >= dateFrom);
    if (dateTo   != null)txs = txs.filter(t => t.date <= dateTo);
    if (minAmt   != null)txs = txs.filter(t => t.amount >= minAmt);
    if (maxAmt   != null)txs = txs.filter(t => t.amount <= maxAmt);
    if (qPayee)          txs = txs.filter(t => (t.payee || '').toLowerCase().includes(qPayee));

    txs.sort((a, b) => b.date - a.date);
    const total = txs.length;
    const items = txs.slice(page * limit, page * limit + limit);
    return { success: true, data: { items, total, page, limit, hasMore: page * limit + limit < total } };
  }

  /* ── override apiFetch ──────────────────────────────────────────────────── */
  window.apiFetch = async function mockApiFetch(path) {
    await delay(jitter());
    // dynamic transaction filtering
    if (normPath(path) === '/api/transactions') {
      return handleTransactions(path);
    }
    // per-transaction lookup by numeric id
    const txMatch = normPath(path).match(/^\/api\/transactions\/(\d+)$/);
    if (txMatch) {
      const id = parseInt(txMatch[1], 10);
      const tx = (window.MOCK_TRANSACTIONS || []).find(t => t.id === id);
      if (tx) return JSON.parse(JSON.stringify({ success: true, data: tx }));
    }
    const fixture = findFixture(path);
    if (fixture) return JSON.parse(JSON.stringify(fixture)); // deep-clone so mutations don't corrupt the store
    console.warn('[mock] No fixture for GET', path);
    return { success: false, error: 'Mock: no fixture for ' + path };
  };

  /* ── override apiWrite ──────────────────────────────────────────────────── */
  window.apiWrite = async function mockApiWrite(method, path, body) {
    await delay(jitter());
    const key = normPath(path);

    // AUTH
    if (key === '/api/auth' && method === 'POST') {
      return { success: true, data: { token: 'mock-token', profileId: 'mock-profile' } };
    }

    // Analytics toggle / filter — mutate in-memory so the pill/toggle responds
    if (key === '/api/analytics/common-filter' && method === 'POST') {
      const d = MOCK_DATA['/api/analytics']?.data;
      if (d) d.commonFilterEnabled = !!body;
      return { success: true };
    }
    if (key === '/api/analytics/filter' && method === 'POST') {
      const d = MOCK_DATA['/api/analytics']?.data;
      if (d && body) d.screenFilter = { ...d.screenFilter, ...body };
      return { success: true };
    }

    // Card filter/settings update — mutate the in-memory card config
    const cardMatch = key.match(/^\/api\/dashboard\/cards\/(.+)$/);
    if (cardMatch && (method === 'PUT' || method === 'PATCH')) {
      const cardId = cardMatch[1];
      const fixture = MOCK_DATA[key];
      if (fixture?.data?.config && body) {
        Object.assign(fixture.data.config, body);
      }
      if (fixture?.data?.data && body) {
        // merge filter-level fields into data payload for immediate re-render
        const filterFields = ['perspective', 'showComparison', 'fontSize', 'layout', 'gridColumns', 'isCumulative', 'selectedMetric', 'selectedNatures'];
        filterFields.forEach(f => { if (body[f] !== undefined) fixture.data.data[f] = body[f]; });
      }
      return { success: true, data: MOCK_DATA[key]?.data };
    }

    // Settings update — mutate in-memory so re-render sees new values
    if (key === '/api/settings' && method === 'PUT' && body) {
      const d = MOCK_DATA['/api/settings']?.data;
      if (d) Object.assign(d, body);
      return { success: true };
    }

    // All other writes (create/update/delete) — optimistic success
    console.info('[mock]', method, path, body);
    return { success: true, data: body || {} };
  };

  /* ── login bypass ───────────────────────────────────────────────────────── */
  // Inject a fake token before state.js reads it (see inline script in index.html).
  // apiWrite POST /api/auth is also mocked above for cases where the user clicks Verify.

  /* ── banner ─────────────────────────────────────────────────────────────── */
  document.addEventListener('DOMContentLoaded', () => {
    const banner = document.createElement('div');
    banner.id = 'mock-banner';
    Object.assign(banner.style, {
      position: 'fixed', top: '0', left: '0', right: '0', zIndex: '9999',
      background: '#7C3AED', color: '#fff', textAlign: 'center',
      fontSize: '0.75rem', fontWeight: '700', padding: '4px 12px',
      letterSpacing: '0.05em', pointerEvents: 'none',
    });
    banner.textContent = '⚡ MOCK MODE — data is simulated, no server required';
    document.body.prepend(banner);
    // Shift the header down so the banner doesn't overlap
    const header = document.getElementById('header');
    if (header) header.style.marginTop = '24px';
  });

})();

/* Boot, auth, navigation, theme toggle — must load last */

/* ── Boot ── */
if (token) { showApp(); warmCache().then(() => route()); }
else { showLogin(); }

document.getElementById('code-input').addEventListener('keydown', e => {
  if (e.key === 'Enter') document.getElementById('login-btn').click();
});
document.getElementById('login-btn').addEventListener('click', doLogin);
document.getElementById('logout-btn').addEventListener('click', doLogout);
document.getElementById('hamburger').addEventListener('click', () => {
  const opening = !sidebar.classList.contains('open');
  sidebar.classList.toggle('open'); overlay.classList.toggle('visible');
  const mc = document.getElementById('content');
  if (mc) { if (opening) mc.setAttribute('inert', ''); else mc.removeAttribute('inert'); }
});
overlay.addEventListener('click', closeSidebar);

(function () {
  const btn = document.getElementById('theme-toggle');
  function applyTheme(theme) {
    document.documentElement.classList.remove('force-light', 'force-dark');
    if (theme === 'light') document.documentElement.classList.add('force-light');
    else if (theme === 'dark') document.documentElement.classList.add('force-dark');
    btn.textContent = (theme === 'light') ? '🌙' : '☀️';
  }
  const saved = localStorage.getItem('oss-theme');
  if (saved) applyTheme(saved);
  btn.addEventListener('click', () => {
    const isLight = document.documentElement.classList.contains('force-light');
    const next = isLight ? 'dark' : 'light';
    applyTheme(next);
    localStorage.setItem('oss-theme', next);
  });
})();

window.addEventListener('hashchange', route);

document.addEventListener('keydown', e => {
  if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
  if (e.key === 'Escape') { closeModal(); }
  if (e.key === 'n' && !e.ctrlKey && !e.metaKey && !e.altKey) {
    if (location.hash.startsWith('#/records') || location.hash.startsWith('#/dashboard') || location.hash === '') {
      openAddTransaction();
    }
  }
});

/* ── Auth ── */
async function doLogin() {
  const btn   = document.getElementById('login-btn');
  const input = document.getElementById('code-input');
  const errEl = document.getElementById('login-error');
  const code  = input.value.trim().toUpperCase();
  if (code.length < 8) { errEl.textContent = 'Enter the full 8-character code.'; return; }
  btn.disabled = true; btn.textContent = 'Verifying…'; errEl.textContent = '';
  try {
    const res  = await fetch(baseUrl + '/api/auth', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accessCode: code })
    });
    const json = await res.json();
    if (res.status === 429) {
      errEl.textContent = json.error || 'Too many attempts. Wait 60 seconds.';
    } else if (json.success && json.data?.token) {
      token = json.data.token;
      sessionStorage.setItem('oss_token', token);
      showApp();
      await warmCache();
      navigate('#/dashboard');
    } else {
      errEl.textContent = 'Incorrect code. Try again.';
    }
  } catch { errEl.textContent = 'Connection error. Is the server still running?'; }
  finally { btn.disabled = false; btn.textContent = 'Verify'; }
}

function doLogout() {
  token = ''; sessionStorage.removeItem('oss_token');
  cachedCategories = []; cachedAccounts = [];
  showLogin();
  document.getElementById('code-input').value = '';
  document.getElementById('login-error').textContent = '';
}

/* ── Cache warm-up ── */
async function warmCache() {
  try {
    const [catR, accR, setR, tagR] = await Promise.allSettled([
      apiFetch('/api/categories'),
      apiFetch('/api/accounts'),
      apiFetch('/api/settings'),
      apiFetch('/api/tags')
    ]);
    if (catR.status === 'fulfilled' && catR.value?.success) cachedCategories = catR.value.data || [];
    if (accR.status === 'fulfilled' && accR.value?.success) cachedAccounts   = accR.value.data || [];
    if (setR.status === 'fulfilled' && setR.value?.success) cachedSettings   = setR.value.data || cachedSettings;
    if (tagR.status === 'fulfilled' && tagR.value?.success) cachedTags = (tagR.value.data || []).map(t => t.name || t).filter(Boolean);
    if (typeof refreshSidebar === 'function') refreshSidebar();
  } catch { /* non-fatal */ }
}

/* ── Navigation ── */
function closeSidebar() {
  sidebar.classList.remove('open'); overlay.classList.remove('visible');
  const mc = document.getElementById('content'); if (mc) mc.removeAttribute('inert');
}

const PAGE_TITLES = {
  '#/dashboard': 'Dashboard', '#/records': 'Records', '#/accounts': 'Accounts',
  '#/budgets': 'Budgets', '#/loans': 'Loans', '#/planned-payments': 'Planned Payments',
  '#/categories': 'Categories', '#/analytics': 'Analytics', '#/tags': 'Tags',
  '#/templates': 'Templates', '#/filters': 'Saved Filters', '#/settings': 'Settings',
};

function route() {
  const hash = location.hash || '#/dashboard';
  const pageName = Object.entries(PAGE_TITLES).find(([k]) => hash.startsWith(k))?.[1] || 'OssWallet';
  document.title = 'OssWallet — ' + pageName;
  updateActiveNav(hash); closeSidebar();
  if      (hash.startsWith('#/dashboard'))        renderDashboard();
  else if (hash.startsWith('#/records'))          renderRecords();
  else if (hash.startsWith('#/accounts/'))        renderAccountDetail(hash.split('/')[2]);
  else if (hash.startsWith('#/accounts'))         renderAccounts();
  else if (hash.startsWith('#/budgets/'))         renderBudgetDetail(hash.split('/')[2]);
  else if (hash.startsWith('#/budgets'))          renderBudgets();
  else if (hash.startsWith('#/loans/'))           renderLoanDetail(hash.split('/')[2]);
  else if (hash.startsWith('#/loans'))            renderLoans();
  else if (hash.startsWith('#/planned-payments')) renderPlannedPayments();
  else if (hash.startsWith('#/categories'))       renderCategories();
  else if (hash.startsWith('#/analytics'))        renderAnalytics();
  else if (hash.startsWith('#/tags'))             renderTags();
  else if (hash.startsWith('#/templates'))        renderTemplates();
  else if (hash.startsWith('#/filters'))          renderFilters();
  else if (hash.startsWith('#/settings'))         renderSettings();
  else renderDashboard();
}

function navigate(hash) { location.hash = hash; }

function updateActiveNav(hash) {
  document.querySelectorAll('.nav-item').forEach(el => {
    const href = el.getAttribute('href');
    el.classList.toggle('active', href && hash.startsWith(href));
  });
  // Hide analytics filter pill when leaving analytics
  if (!hash.startsWith('#/analytics')) {
    document.getElementById('analytics-filter-pill')?.classList.remove('visible');
  }
}

window.doLogout = doLogout;

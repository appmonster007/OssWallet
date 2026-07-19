/* UI helpers — DOM refs, page show/hide, toast, progress, validation */

const loginPage = document.getElementById('login-page');
const appPage   = document.getElementById('app');
const content   = document.getElementById('content');
const sidebar   = document.getElementById('sidebar');
const overlay   = document.getElementById('overlay');

function showLogin() {
  loginPage.style.display = 'flex';
  appPage.style.display = 'none';
  setTimeout(() => document.getElementById('code-input').focus(), 100);
}
function showApp() {
  loginPage.style.display = 'none';
  appPage.style.display = 'flex';
}

function setContent(html) {
  content.innerHTML = html;
  endProgress();
  requestAnimationFrame(() => {
    document.querySelectorAll('.progress-fill[data-pct]').forEach(el => {
      el.style.width = (el.dataset.pct || '0') + '%';
    });
  });
}
function showSpinner() { startProgress(); setContent('<div class="loading-wrap"><div class="spinner"></div></div>'); }
function showError(msg) { endProgress(); setContent(`<div class="alert alert-error">${esc(msg)}</div>`); }

function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(24px)';
    setTimeout(() => toast.remove(), 300);
  }, 3200);
}

function startProgress() {
  const el = document.getElementById('nprogress');
  if (!el) return;
  el.style.transition = 'none';
  el.style.width = '0';
  el.style.opacity = '1';
  requestAnimationFrame(() => {
    el.style.transition = 'width 1.2s cubic-bezier(.4,0,.2,1)';
    el.style.width = '75%';
  });
}
function endProgress() {
  const el = document.getElementById('nprogress');
  if (!el) return;
  el.style.transition = 'width .2s ease, opacity .3s ease';
  el.style.width = '100%';
  setTimeout(() => { el.style.opacity = '0'; setTimeout(() => { el.style.width = '0'; el.style.transition = 'none'; }, 300); }, 200);
}

async function withLoading(btn, fn) {
  const orig = btn.innerHTML;
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner" style="width:14px;height:14px;border-width:2px;display:inline-block;vertical-align:middle;margin-right:4px"></span>Saving…';
  try { await fn(); } finally { btn.disabled = false; btn.innerHTML = orig; }
}

function showFieldError(elId, msg) {
  const el = typeof elId === 'string' ? document.getElementById(elId) : elId;
  if (!el) return;
  el.classList.add('field-error');
  const prev = el.parentNode && el.parentNode.querySelector('.field-error-msg');
  if (prev) prev.remove();
  const span = document.createElement('span');
  span.className = 'field-error-msg';
  span.textContent = msg;
  if (el.parentNode) el.parentNode.insertBefore(span, el.nextSibling);
  el.focus();
}
function clearFieldErrors(container) {
  if (!container) return;
  container.querySelectorAll('.field-error').forEach(el => el.classList.remove('field-error'));
  container.querySelectorAll('.field-error-msg').forEach(el => el.remove());
}

/* PAGE: Settings */

async function renderSettings() {
  showSpinner();
  try {
    const json = await apiFetch('/api/settings');
    if (!json.success) { showError('Failed to load settings.'); return; }
    const s = json.data || {};
    cachedSettings = { ...cachedSettings, ...s };

    const inputStyle = 'padding:4px 8px;border:1px solid var(--border);border-radius:var(--radius-sm);background:var(--surface2);color:var(--text);font-size:0.9rem';
    const savedTheme = localStorage.getItem('oss-theme') || 'auto';
    const savedPageSize = localStorage.getItem('oss-page-size') || '25';

    const html = `<h2 class="section-title">Settings</h2>
      <div class="card" style="max-width:520px">

        <div class="form-section-label">Profile</div>
        <div class="settings-row">
          <div>
            <div class="settings-label">Profile ID</div>
            <div class="settings-value" style="font-size:0.78rem;margin-top:2px">Active profile</div>
          </div>
          <div style="display:flex;align-items:center;gap:8px">
            <span class="settings-value" style="font-family:monospace">${esc(String(s.currentProfileId || '—'))}</span>
            <button class="btn btn-ghost btn-sm" style="font-size:0.78rem;padding:3px 8px" onclick="navigator.clipboard.writeText('${esc(String(s.currentProfileId||''))}').then(()=>showToast('Profile ID copied!'))">Copy</button>
          </div>
        </div>
        <div class="settings-row">
          <div>
            <div class="settings-label">Timezone</div>
            <div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">Detected from browser</div>
          </div>
          <span class="settings-value" style="font-size:0.82rem">${esc(Intl.DateTimeFormat().resolvedOptions().timeZone || '—')}</span>
        </div>

        <div class="form-section-label" style="margin-top:20px">Currency &amp; Display</div>
        <div class="settings-row">
          <div style="flex:1">
            <div class="settings-label">Base Currency</div>
            <div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">Default currency for new entries</div>
          </div>
          <input type="text" id="set-currency" value="${esc(s.baseCurrency||'')}" maxlength="5" style="width:80px;${inputStyle}">
        </div>
        <div class="settings-row">
          <div style="flex:1">
            <div class="settings-label">Decimal Precision</div>
            <div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">Decimal places for amounts</div>
          </div>
          <input type="number" id="set-precision" value="${esc(String(s.decimalPrecision ?? 2))}" min="0" max="8" step="1" style="width:70px;${inputStyle}">
        </div>
        <div class="settings-row">
          <div style="flex:1">
            <div class="settings-label">Date Format</div>
            <div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">How dates are displayed</div>
          </div>
          <select id="set-date-format" style="${inputStyle}">
            <option value="DD/MM/YYYY" ${(s.dateFormat||'DD/MM/YYYY')==='DD/MM/YYYY'?'selected':''}>DD/MM/YYYY</option>
            <option value="MM/DD/YYYY" ${s.dateFormat==='MM/DD/YYYY'?'selected':''}>MM/DD/YYYY</option>
            <option value="YYYY-MM-DD" ${s.dateFormat==='YYYY-MM-DD'?'selected':''}>YYYY-MM-DD</option>
          </select>
        </div>
        <div class="settings-row" style="justify-content:flex-end;gap:8px;padding-top:4px">
          <button class="btn btn-primary btn-sm" id="set-save-btn" onclick="saveSettings()">Save Settings</button>
        </div>

        <div class="form-section-label" style="margin-top:20px">Appearance</div>
        <div class="settings-row">
          <div style="flex:1">
            <div class="settings-label">Theme</div>
            <div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">Light, dark, or follow system preference</div>
          </div>
          <div style="display:flex;gap:6px">
            ${['auto','light','dark'].map(t=>`<button id="theme-btn-${t}" class="btn btn-sm ${savedTheme===t?'btn-primary':'btn-ghost'}" onclick="setThemePref('${t}')">${t==='auto'?' Auto':t==='light'?'Light':'Dark'}</button>`).join('')}
          </div>
        </div>
        <div class="settings-row">
          <div style="flex:1">
            <div class="settings-label">Records Per Page</div>
            <div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">Default rows shown in Records list</div>
          </div>
          <select id="set-page-size" style="${inputStyle}" onchange="localStorage.setItem('oss-page-size',this.value);showToast('Page size saved!')">
            ${[10,25,50,100].map(n=>`<option value="${n}" ${savedPageSize==n?'selected':''}>${n} rows</option>`).join('')}
          </select>
        </div>

        <div class="form-section-label" style="margin-top:20px">Security</div>
        <div class="settings-row">
          <div>
            <div class="settings-label">Session</div>
            <div style="font-size:0.78rem;color:var(--text-muted);margin-top:2px">Stored in sessionStorage (cleared on tab close)</div>
          </div>
          <button class="btn btn-danger btn-sm" onclick="doLogout()">Log out</button>
        </div>
      </div>`;

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading settings.'); }
}

window.setThemePref = function (theme) {
  localStorage.setItem('oss-theme', theme);
  // Apply immediately using same logic as app.js theme toggle
  document.documentElement.classList.remove('force-light', 'force-dark');
  if (theme === 'light') document.documentElement.classList.add('force-light');
  else if (theme === 'dark') document.documentElement.classList.add('force-dark');
  const headerBtn = document.getElementById('theme-toggle');
  if (headerBtn) headerBtn.textContent = (theme === 'light') ? 'Dark' : 'Light';
  // Refresh button states
  ['auto','light','dark'].forEach(t => {
    const btn = document.getElementById('theme-btn-' + t);
    if (btn) { btn.className = 'btn btn-sm ' + (t === theme ? 'btn-primary' : 'btn-ghost'); }
  });
  showToast(`Theme set to ${theme}.`);
};

window.saveSettings = async function () {
  const currency   = document.getElementById('set-currency')?.value.trim();
  const precision  = parseInt(document.getElementById('set-precision')?.value || '2', 10);
  const dateFormat = document.getElementById('set-date-format')?.value;
  if (!currency) { showFieldError('set-currency', 'Currency is required.'); return; }
  if (isNaN(precision) || precision < 0 || precision > 8) { showFieldError('set-precision', 'Must be 0–8.'); return; }
  const btn = document.getElementById('set-save-btn');
  if (btn) await withLoading(btn, async () => {
    const payload = { baseCurrency: currency, decimalPrecision: precision };
    if (dateFormat) payload.dateFormat = dateFormat;
    const res = await apiWrite('PUT', '/api/settings', payload);
    if (res.success) {
      cachedSettings = { ...cachedSettings, baseCurrency: currency, decimalPrecision: precision, dateFormat };
      showToast('Settings saved!');
    } else { showToast('Failed to save settings: ' + (res.error || ''), 'error'); }
  });
};

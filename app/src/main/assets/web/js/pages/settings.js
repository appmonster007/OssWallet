/* PAGE: Settings */

async function renderSettings() {
  showSpinner();
  try {
    const json = await apiFetch('/api/settings');
    if (!json.success) { showError('Failed to load settings.'); return; }
    const s = json.data || {};
    cachedSettings = { ...cachedSettings, ...s };

    const savedTheme    = s.theme || localStorage.getItem('oss-theme') || 'system';
    const savedPageSize = localStorage.getItem('oss-page-size') || '25';
    const fow           = s.firstDayOfWeek || 'MONDAY';

    const dateFormatOpts = [
      { value: 'MMM d, yyyy',  label: 'Jan 5, 2026' },
      { value: 'd MMM yyyy',   label: '5 Jan 2026'  },
      { value: 'dd/MM/yyyy',   label: '05/01/2026'  },
      { value: 'MM/dd/yyyy',   label: '01/05/2026'  },
      { value: 'yyyy-MM-dd',   label: '2026-01-05'  },
    ];
    const currentDateFmt = s.dateFormat || 'MMM d, yyyy';

    const langOpts = [
      { value: 'en', label: 'English' },
      { value: 'id', label: 'Bahasa Indonesia' },
      { value: 'ja', label: '日本語' },
    ];
    const currentLang = s.language || 'en';

    const html = `
    <div class="page-header">
      <h2 class="section-title">Settings</h2>
    </div>

    <div class="settings-page">

      <!-- ── Profile ──────────────────────────── -->
      <div class="settings-section">
        <div class="settings-section-title">Profile</div>
        <div class="card settings-card">
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">Profile Name</div>
              <div class="settings-hint">Displayed across the app</div>
            </div>
            <input type="text" id="set-profile-name" class="settings-input"
              value="${esc(s.profileName || '')}" placeholder="My Profile" maxlength="64">
          </div>
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">Profile ID</div>
              <div class="settings-hint">Active profile identifier</div>
            </div>
            <div style="display:flex;align-items:center;gap:8px">
              <span class="settings-value" style="font-family:monospace;font-size:0.82rem">${esc(String(s.currentProfileId || '1'))}</span>
              <button class="btn btn-ghost btn-sm" style="font-size:0.75rem;padding:2px 7px"
                onclick="navigator.clipboard.writeText('${esc(String(s.currentProfileId||'1'))}').then(()=>showToast('Copied!'))">Copy</button>
            </div>
          </div>
          <div class="settings-row" style="border-bottom:none">
            <div class="settings-row-label">
              <div class="settings-label">Timezone</div>
              <div class="settings-hint">Detected from browser</div>
            </div>
            <span class="settings-value settings-value-mono">${esc(Intl.DateTimeFormat().resolvedOptions().timeZone || '—')}</span>
          </div>
        </div>
      </div>

      <!-- ── Currency & Display ─────────────────── -->
      <div class="settings-section">
        <div class="settings-section-title">Currency &amp; Display</div>
        <div class="card settings-card">
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">Base Currency</div>
              <div class="settings-hint">Default for new transactions</div>
            </div>
            <input type="text" id="set-currency" class="settings-input settings-input-sm"
              value="${esc(s.baseCurrency || 'USD')}" maxlength="5" placeholder="USD"
              style="text-transform:uppercase" oninput="this.value=this.value.toUpperCase()">
          </div>
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">Decimal Precision</div>
              <div class="settings-hint">Decimal places shown on amounts</div>
            </div>
            <input type="number" id="set-precision" class="settings-input settings-input-sm"
              value="${esc(String(s.decimalPrecision ?? 2))}" min="0" max="4" step="1">
          </div>
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">Date Format</div>
              <div class="settings-hint">How dates are displayed throughout the app</div>
            </div>
            <div style="min-width:0">
              ${buildSelect('set-date-format', dateFormatOpts, currentDateFmt, '', false)}
            </div>
          </div>
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">First Day of Week</div>
              <div class="settings-hint">Used in weekly views and calendars</div>
            </div>
            <div class="chip-group" style="gap:4px">
              ${[['MONDAY','Mon'],['SATURDAY','Sat'],['SUNDAY','Sun']].map(([v,l])=>`
                <label class="chip-check ${fow===v?'active':''}">
                  <input type="radio" name="set-fow" value="${v}" ${fow===v?'checked':''}
                    onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(c=>c.classList.remove('active'));this.closest('label').classList.add('active')">
                  <span>${l}</span>
                </label>`).join('')}
            </div>
          </div>
          <div class="settings-row" style="border-bottom:none">
            <div class="settings-row-label">
              <div class="settings-label">Language</div>
              <div class="settings-hint">App display language</div>
            </div>
            <div>
              ${buildSelect('set-language', langOpts, currentLang, '', false)}
            </div>
          </div>
        </div>
        <div style="display:flex;justify-content:flex-end;margin-top:10px">
          <button class="btn btn-primary" id="set-save-btn" onclick="saveSettings()">Save Changes</button>
        </div>
      </div>

      <!-- ── Appearance ─────────────────────────── -->
      <div class="settings-section">
        <div class="settings-section-title">Appearance</div>
        <div class="card settings-card">
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">Theme</div>
              <div class="settings-hint">Light, dark, or follow system preference</div>
            </div>
            <div class="chip-group" style="gap:4px">
              ${[['system','Auto'],['light','Light'],['dark','Dark']].map(([v,l])=>`
                <label class="chip-check ${savedTheme===v?'active':''}">
                  <input type="radio" name="set-theme" value="${v}" ${savedTheme===v?'checked':''}
                    onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(c=>c.classList.remove('active'));this.closest('label').classList.add('active');setThemePref('${v}')">
                  <span>${l}</span>
                </label>`).join('')}
            </div>
          </div>
          <div class="settings-row" style="border-bottom:none">
            <div class="settings-row-label">
              <div class="settings-label">Records Per Page</div>
              <div class="settings-hint">Default rows shown in the Records list</div>
            </div>
            <select id="set-page-size" class="settings-input"
              onchange="localStorage.setItem('oss-page-size',this.value);showToast('Page size saved!')">
              ${[10,25,50,100].map(n=>`<option value="${n}" ${savedPageSize==n?'selected':''}>${n} rows</option>`).join('')}
            </select>
          </div>
        </div>
      </div>

      <!-- ── Security & Access ──────────────────── -->
      <div class="settings-section">
        <div class="settings-section-title">Security &amp; Access</div>
        <div class="card settings-card">
          <div class="settings-row">
            <div class="settings-row-label">
              <div class="settings-label">Web Server</div>
              <div class="settings-hint">Remote web UI access from this device</div>
            </div>
            <span class="settings-badge ${s.enableWebServer ? 'settings-badge-on' : 'settings-badge-off'}">
              ${s.enableWebServer ? 'Enabled' : 'Disabled'}
            </span>
          </div>
          <div class="settings-row" style="border-bottom:none">
            <div class="settings-row-label">
              <div class="settings-label">Session</div>
              <div class="settings-hint">Stored in sessionStorage — cleared on tab close</div>
            </div>
            <button class="btn btn-danger btn-sm" onclick="doLogout()">Log out</button>
          </div>
        </div>
      </div>

    </div>`;

    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading settings.'); }
}

window.setThemePref = function (theme) {
  localStorage.setItem('oss-theme', theme);
  document.documentElement.classList.remove('force-light', 'force-dark');
  if (theme === 'light') document.documentElement.classList.add('force-light');
  else if (theme === 'dark') document.documentElement.classList.add('force-dark');
  const headerBtn = document.getElementById('theme-toggle');
  if (headerBtn) headerBtn.textContent = theme === 'light' ? 'Dark' : 'Light';
  showToast(`Theme set to ${theme}.`);
};

window.saveSettings = async function () {
  const profileName = document.getElementById('set-profile-name')?.value.trim();
  const currency    = document.getElementById('set-currency')?.value.trim().toUpperCase();
  const precision   = parseInt(document.getElementById('set-precision')?.value || '2', 10);
  const dateFormat  = document.getElementById('set-date-format')?.value;
  const fow         = document.querySelector('input[name="set-fow"]:checked')?.value || 'MONDAY';
  const language    = document.getElementById('set-language')?.value || 'en';

  if (!currency)              { showFieldError('set-currency',    'Currency is required.');   return; }
  if (isNaN(precision) || precision < 0 || precision > 4) {
                                showFieldError('set-precision',   'Must be 0–4.');             return; }

  const btn = document.getElementById('set-save-btn');
  if (btn) await withLoading(btn, async () => {
    const payload = { profileName, baseCurrency: currency, decimalPrecision: precision, dateFormat, firstDayOfWeek: fow, language };
    const res = await apiWrite('PUT', '/api/settings', payload);
    if (res.success) {
      cachedSettings = { ...cachedSettings, ...payload };
      showToast('Settings saved!');
    } else {
      showToast('Failed to save: ' + (res.error || ''), 'error');
    }
  });
};

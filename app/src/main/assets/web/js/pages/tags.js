/* PAGE: Tags */

async function renderTags(searchQuery) {
  showSpinner();
  try {
    const json = await apiFetch('/api/tags');
    if (!json.success) { showError('Failed to load tags.'); return; }
    const allTags = json.data || [];
    const q = (searchQuery || '').toLowerCase().trim();
    const tags = q ? allTags.filter(t => (t.name || t).toLowerCase().includes(q)) : allTags;

    let html = `<div class="page-header">
      <h2 class="section-title">Tags</h2>
    </div>
    <p style="color:var(--text-muted);font-size:0.88rem;margin-bottom:16px">Tags are created automatically when added to records. Rename or delete unused tags here.</p>
    <div class="search-bar-wrap" style="margin-bottom:16px">
      <input type="text" class="search-bar" id="tag-search" placeholder="Search tags…"
        value="${esc(q)}"
        oninput="renderTags(this.value)">
    </div>`;

    if (!allTags.length) {
      html += emptyState('', 'No tags yet. Tags appear here when added to a record.', null, null);
      setContent(html);
      return;
    }

    if (!tags.length) {
      html += `<div style="text-align:center;padding:32px;color:var(--text-muted)">No tags match "<strong>${esc(q)}</strong>"</div>`;
      setContent(html);
      document.getElementById('tag-search')?.focus();
      return;
    }

    html += `<div class="tags-grid">`;
    tags.forEach(tag => {
      const name = tag.name || tag;
      const col  = tag.colorHex ? resolveTagColor(tag.colorHex) : tagColorFromName(name);
      const count = tag.transactionCount != null ? tag.transactionCount : null;
      html += `<div class="tag-card" style="border-top:3px solid ${esc(col)}">
        <div class="tag-card-dot" style="background:${esc(col)}">${esc(name.charAt(0).toUpperCase())}</div>
        <div class="tag-card-name">${esc(name)}</div>
        ${count != null ? `<div class="tag-card-count">${count} record${count !== 1 ? 's' : ''}</div>` : ''}
        <div class="tag-card-actions">
          <button class="btn btn-sm" style="padding:3px 8px;font-size:0.75rem" onclick="openRenameTag('${esc(name)}','${esc(col)}')">Rename</button>
          <button class="btn-icon" title="Delete" style="color:var(--danger)" onclick="deleteTag('${esc(name)}')">✕</button>
        </div>
      </div>`;
    });
    html += `</div>`;

    setContent(html);
    const inp = document.getElementById('tag-search');
    if (inp && q) { inp.focus(); inp.setSelectionRange(q.length, q.length); }
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading tags.'); }
}

function resolveTagColor(colorHex) {
  if (typeof colorHex === 'number') return '#' + ((colorHex >>> 0) & 0xFFFFFF).toString(16).padStart(6,'0');
  return colorHex || '#9e9e9e';
}

// Generate a deterministic accent color from the tag name when no colorHex is set
function tagColorFromName(name) {
  const palette = [
    '#1976D2','#2e7d32','#c62828','#f57f17','#6a1b9a','#00838f',
    '#ad1457','#4e342e','#f57c00','#1565c0','#7b1fa2','#00695c',
    '#880e4f','#37474f','#1b5e20','#33691e','#311b92','#0277bd',
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) & 0xFFFF;
  return palette[hash % palette.length];
}

window.openRenameTag = function (name, colorHex) {
  const palette = [
    '#1976D2','#2e7d32','#c62828','#f57f17','#6a1b9a','#00838f',
    '#ad1457','#4e342e','#f57c00','#1565c0','#7b1fa2','#00695c',
  ];
  const colorBtns = palette.map(c =>
    `<span class="tag-color-btn" style="background:${c};width:24px;height:24px;border-radius:50%;cursor:pointer;display:inline-block;border:2px solid ${c===colorHex?'var(--accent)':'transparent'};flex-shrink:0"
      onclick="document.getElementById('tag-color').value='${c}';document.querySelectorAll('.tag-color-btn').forEach(s=>s.style.borderColor=s.getAttribute('data-col')==='${c}'?'var(--accent)':'transparent')" data-col="${c}"></span>`
  ).join('');
  const body = `
    ${formField('New Name', `<input type="text" id="tag-new-name" value="${esc(name)}" placeholder="Tag name" autofocus>`)}
    <div class="form-field"><label>Color</label>
      <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:8px">${colorBtns}</div>
      <div style="display:flex;align-items:center;gap:10px">
        <input type="color" id="tag-color" value="${esc(colorHex || '#1976D2')}" style="width:40px;height:32px;border:none;border-radius:4px;cursor:pointer;padding:0">
        <span style="font-size:0.85rem;color:var(--text-muted)">or pick custom colour</span>
      </div>
    </div>`;
  openModal(`Rename — "${esc(name)}"`, body, async () => {
    const newName  = document.getElementById('tag-new-name')?.value.trim();
    const newColor = document.getElementById('tag-color')?.value || colorHex;
    if (!newName) { showFieldError('tag-new-name', 'Name is required.'); return; }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/tags/${encodeURIComponent(name)}`, { newName, colorHex: newColor });
      if (res.success) { closeModal(); await warmCache(); renderTags(); showToast('Tag renamed!'); }
      else { showToast('Failed to rename: ' + (res.error || ''), 'error'); }
    });
  }, 'Save');
  modalConfBtn.className = 'btn btn-primary';
};

window.deleteTag = function (name) {
  confirmDelete(`Delete tag "${name}"? It will be removed from all records.`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/tags/${encodeURIComponent(name)}`);
      closeModal();
      if (res.success) { await warmCache(); renderTags(); showToast('Tag deleted.'); }
      else { showToast('Failed to delete tag.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting tag.', 'error'); }
  });
};

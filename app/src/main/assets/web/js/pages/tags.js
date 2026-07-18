/* PAGE: Tags */

async function renderTags() {
  showSpinner();
  try {
    const json = await apiFetch('/api/tags');
    if (!json.success) { showError('Failed to load tags.'); return; }
    const tags = json.data || [];

    let html = `<div class="page-header"><h2 class="section-title">Tags</h2></div>`;
    html += '<p style="color:var(--text-muted);font-size:0.88rem;margin-bottom:16px">Tags are created automatically when you add them to records. Rename or delete unused tags here.</p>';

    if (!tags.length) {
      html += emptyState('', 'No tags yet.', 'Add a Record with Tags', 'openAddTransaction()');
    } else {
      html += '<div class="card"><div class="tag-chips">';
      tags.forEach(tag => {
        const col = tag.colorHex || '#9e9e9e';
        html += `<div class="tag-chip" style="border-left:4px solid ${esc(col)}">
          <span class="color-swatch" style="background:${esc(col)}"></span>
          <span>${esc(tag.name)}</span>
          <span class="chip-count">${tag.transactionCount || 0} records</span>
          <button class="btn-icon" title="Rename tag" style="padding:2px 4px" onclick="openRenameTag('${esc(tag.name)}','${esc(col)}')">Edit</button>
          <button class="btn-icon" title="Delete tag" style="padding:2px 4px" onclick="deleteTag('${esc(tag.name)}')">✕</button>
        </div>`;
      });
      html += '</div></div>';
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading tags.'); }
}

window.openRenameTag = function (name, colorHex) {
  const body = `
    ${formField('New Name', `<input type="text" id="tag-new-name" value="${esc(name)}" placeholder="Tag name">`)}
    <div class="form-field"><label>Color</label>
      <div style="display:flex;align-items:center;gap:10px">
        <input type="color" id="tag-color" value="${esc(colorHex || '#9e9e9e')}" style="width:40px;height:32px;border:none;border-radius:4px;cursor:pointer;padding:0">
        <span style="font-size:0.85rem;color:var(--text-muted)">Pick a colour for this tag</span>
      </div>
    </div>`;
  openModal(`Rename Tag — "${esc(name)}"`, body, async () => {
    const newName = document.getElementById('tag-new-name')?.value.trim();
    const newColor = document.getElementById('tag-color')?.value || colorHex;
    if (!newName) { showFieldError('tag-new-name', 'Name is required.'); return; }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/tags/${encodeURIComponent(name)}`, { newName, colorHex: newColor });
      if (res.success) {
        closeModal();
        await warmCache();
        renderTags();
        showToast('Tag renamed!');
      } else {
        showToast('Failed to rename tag: ' + (res.error || ''), 'error');
      }
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

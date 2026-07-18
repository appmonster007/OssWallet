/* PAGE: Categories */

async function renderCategories() {
  showSpinner();
  try {
    const json = await apiFetch('/api/categories');
    if (!json.success) { showError('Failed to load categories.'); return; }
    cachedCategories = json.data || [];
    const parents  = cachedCategories.filter(c => !c.parentId);
    const children = cachedCategories.filter(c =>  c.parentId);

    let html = `<div class="page-header"><h2 class="section-title">Categories</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddCategory()">+ Add Category</button>
    </div>`;

    if (!cachedCategories.length) {
      html += emptyState('', 'No categories found.', '+ Add Category', 'openAddCategory()');
    } else {
      html += '<div class="card">';
      parents.forEach((cat, idx) => {
        html += renderCatRow(cat, false, idx, parents.length);
        children.filter(c => c.parentId === cat.id).forEach((child, cidx, arr) => {
          html += renderCatRow(child, true, cidx, arr.length);
        });
      });
      children.filter(c => !parents.find(p => p.id === c.parentId)).forEach((child, cidx, arr) => {
        html += renderCatRow(child, true, cidx, arr.length);
      });
      html += '</div>';
    }
    setContent(html);
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading categories.'); }
}

function renderCatRow(cat, indent, idx, total) {
  const natureCls = cat.nature === 'MUST' ? 'badge-must' : cat.nature === 'NEED' ? 'badge-need' : 'badge-want';
  const color     = typeof cat.colorHex === 'number'
    ? '#' + cat.colorHex.toString(16).padStart(6,'0')
    : (cat.colorHex || '#9e9e9e');
  const iconStr   = cat.iconName && cat.iconName.length <= 2 ? cat.iconName : '📁';
  const deleteBtn = !cat.isDefault
    ? `<button class="btn-icon" title="Delete" onclick="deleteCategory('${esc(cat.id)}','${esc(cat.name)}')">Del</button>` : '';
  const upBtn   = (!indent && idx > 0)
    ? `<button class="btn-icon" title="Move up" onclick="moveCategoryUp(${cat.id})" style="font-size:0.8rem">↑</button>` : '<span style="width:32px;display:inline-block"></span>';
  const downBtn = (!indent && idx < total - 1)
    ? `<button class="btn-icon" title="Move down" onclick="moveCategoryDown(${cat.id})" style="font-size:0.8rem">↓</button>` : '<span style="width:32px;display:inline-block"></span>';

  return `<div style="display:flex;align-items:center;padding:10px 0;border-bottom:1px solid var(--border);${indent ? 'padding-left:28px' : ''}">
    <span style="font-size:1.2rem;min-width:24px;text-align:center;margin-right:8px">${iconStr}</span>
    <span class="color-swatch" style="background:${esc(color)}"></span>
    <span style="flex:1;font-weight:${indent?'400':'600'}">${esc(cat.name)}</span>
    ${cat.nature ? `<span class="badge ${natureCls}" style="margin-right:6px">${esc(cat.nature)}</span>` : ''}
    ${cat.isDefault ? '<span style="font-size:0.75rem;color:var(--text-muted);margin-right:8px">Default</span>' : ''}
    ${!indent ? upBtn : ''}
    ${!indent ? downBtn : ''}
    <button class="btn-icon" title="Edit" onclick="openEditCategory('${esc(cat.id)}')">Edit</button>
    ${deleteBtn}
  </div>`;
}

window.moveCategoryUp = async function (id) {
  const parents = cachedCategories.filter(c => !c.parentId);
  const idx = parents.findIndex(c => c.id === id);
  if (idx <= 0) return;
  const reordered = [...parents];
  [reordered[idx-1], reordered[idx]] = [reordered[idx], reordered[idx-1]];
  await reorderCategories(reordered.map(c => c.id));
};

window.moveCategoryDown = async function (id) {
  const parents = cachedCategories.filter(c => !c.parentId);
  const idx = parents.findIndex(c => c.id === id);
  if (idx < 0 || idx >= parents.length - 1) return;
  const reordered = [...parents];
  [reordered[idx], reordered[idx+1]] = [reordered[idx+1], reordered[idx]];
  await reorderCategories(reordered.map(c => c.id));
};

async function reorderCategories(orderedIds) {
  try {
    const res = await apiWrite('PUT', '/api/categories/reorder', { categoryIds: orderedIds });
    if (res.success) { await warmCache(); renderCategories(); }
    else { showToast('Failed to reorder categories.', 'error'); }
  } catch (e) { if (e.message !== 'Session expired') showToast('Error reordering.', 'error'); }
}

window.openAddCategory = function () {
  const presetColors = ['#1976D2','#2e7d32','#c62828','#f57f17','#6a1b9a','#00838f','#ad1457','#4e342e'];
  const colorBtns = presetColors.map(c =>
    `<span class="color-swatch" style="background:${c};width:22px;height:22px;cursor:pointer;border:2px solid transparent"
      onclick="document.getElementById('cat-color').value='${c}';document.querySelectorAll('.cat-color-btn').forEach(s=>s.style.borderColor='transparent');this.style.borderColor='var(--accent)'"
      class="cat-color-btn"></span>`
  ).join(' ');
  const body = `
    ${formField('Category Name', '<input type="text" id="cat-name" placeholder="e.g. Groceries">')}
    <div class="form-field"><label>Icon (emoji)</label>
      <div style="display:flex;align-items:center;gap:12px">
        <span id="cat-icon-preview" style="font-size:2rem;min-width:40px;text-align:center">📁</span>
        <input type="text" id="cat-icon" placeholder="📁" maxlength="2" style="width:64px;font-size:1.5rem;text-align:center" oninput="const p=document.getElementById('cat-icon-preview');if(p)p.textContent=this.value||'📁'">
      </div>
    </div>
    ${formField('Parent (optional)', buildSelect('cat-parent',[{value:'',label:'— Top-level —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),'','— Top-level —',true))}
    <div class="form-field"><label data-tip="MUST=essential · NEED=important · WANT=discretionary">Nature</label>${buildSelect('cat-nature',[{value:'NEED',label:'Need'},{value:'MUST',label:'Must'},{value:'WANT',label:'Want'}],'NEED','— Select —',false)}</div>
    <div class="form-field"><label>Type</label><div class="chip-group">
      <label class="chip-check active"><input type="radio" name="cat-type" value="EXPENSE" checked onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Expense</span></label>
      <label class="chip-check"><input type="radio" name="cat-type" value="INCOME" onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Income</span></label>
    </div></div>
    <div class="form-field"><label>Color</label>
      <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:8px">${colorBtns}</div>
      <input type="text" id="cat-color" value="${presetColors[0]}" placeholder="#1976D2" maxlength="7">
    </div>`;
  openModal('Add Category', body, submitAddCategory, 'Save Category');
  modalConfBtn.className = 'btn btn-primary';
};

window.submitAddCategory = async function () {
  clearFieldErrors(modalBody);
  const name     = document.getElementById('cat-name')?.value.trim();
  const parentVal = document.getElementById('cat-parent')?.value || '';
  const nature   = document.getElementById('cat-nature')?.value;
  const color    = document.getElementById('cat-color')?.value.trim();
  const catType  = document.querySelector('input[name="cat-type"]:checked')?.value || 'EXPENSE';
  const iconName = document.getElementById('cat-icon')?.value?.trim() || '📁';
  if (!name) { showFieldError('cat-name', 'Name is required.'); return; }
  const existingName = (cachedCategories || []).find(c =>
    c.name.toLowerCase() === name.toLowerCase() &&
    (String(c.parentId||'') === String(parentVal||''))
  );
  if (existingName) {
    showToast(`A category named "${name}" already exists at this level.`, 'info');
  }
  await withLoading(modalConfBtn, async () => {
    const res = await apiWrite('POST', '/api/categories', { name, parentId: parentVal || undefined, nature, colorHex: color || undefined, type: catType, iconName });
    if (res.success) { closeModal(); await warmCache(); renderCategories(); showToast('Category created!'); }
    else { showToast('Failed to create category: ' + (res.error || ''), 'error'); }
  });
};

window.openEditCategory = function (id) {
  const catId = Number(id);
  const cat = cachedCategories.find(c => c.id === catId);
  if (!cat) { showToast('Category not found.', 'error'); return; }
  const presetColors = ['#1976D2','#2e7d32','#c62828','#f57f17','#6a1b9a','#00838f','#ad1457','#4e342e'];
  const colorBtns = presetColors.map(c =>
    `<span class="color-swatch" style="background:${c};width:22px;height:22px;cursor:pointer;border:2px solid transparent"
      onclick="document.getElementById('cat-color').value='${c}';document.querySelectorAll('.cat-color-btn').forEach(s=>s.style.borderColor='transparent');this.style.borderColor='var(--accent)'"
      class="cat-color-btn"></span>`
  ).join(' ');
  const currentColor = typeof cat.colorHex === 'number' ? '#' + cat.colorHex.toString(16).padStart(6,'0') : (cat.colorHex || '#9e9e9e');
  const editCatType = cat.type || 'EXPENSE';
  const catIconVal = cat.iconName || '📁';
  const body = `
    ${formField('Category Name', `<input type="text" id="cat-name" value="${esc(cat.name)}">`)}
    <div class="form-field"><label>Icon (emoji)</label>
      <div style="display:flex;align-items:center;gap:12px">
        <span id="cat-icon-preview" style="font-size:2rem;min-width:40px;text-align:center">${esc(catIconVal)}</span>
        <input type="text" id="cat-icon" value="${esc(catIconVal)}" maxlength="2" style="width:64px;font-size:1.5rem;text-align:center" oninput="const p=document.getElementById('cat-icon-preview');if(p)p.textContent=this.value||'📁'">
      </div>
    </div>
    ${formField('Parent (optional)', buildSelect('cat-parent',[{value:'',label:'— Top-level —'}].concat(cachedCategories.filter(c=>c.id!==catId&&c.parentId!==catId).map(c=>({value:String(c.id),label:c.name}))),cat.parentId?String(cat.parentId):'','— Top-level —',true))}
    <div class="form-field"><label data-tip="MUST=essential · NEED=important · WANT=discretionary">Nature</label>${buildSelect('cat-nature',[{value:'NEED',label:'Need'},{value:'MUST',label:'Must'},{value:'WANT',label:'Want'}],cat.nature||'NEED','— Select —',false)}</div>
    <div class="form-field"><label>Type</label><div class="chip-group">
      <label class="chip-check ${editCatType==='EXPENSE'?'active':''}"><input type="radio" name="cat-type" value="EXPENSE" ${editCatType==='EXPENSE'?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Expense</span></label>
      <label class="chip-check ${editCatType==='INCOME'?'active':''}"><input type="radio" name="cat-type" value="INCOME" ${editCatType==='INCOME'?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Income</span></label>
    </div></div>
    <div class="form-field"><label>Color</label>
      <div style="display:flex;gap:6px;align-items:center;flex-wrap:wrap">${colorBtns}</div>
      <input type="text" id="cat-color" value="${esc(currentColor)}" placeholder="#1976D2" style="width:100px;margin-top:8px">
    </div>`;
  openModal(`Edit Category — ${esc(cat.name)}`, body, async () => {
    clearFieldErrors(modalBody);
    const name     = document.getElementById('cat-name')?.value.trim();
    const parent   = document.getElementById('cat-parent')?.value || null;
    const nature   = document.getElementById('cat-nature')?.value;
    const color    = document.getElementById('cat-color')?.value.trim();
    const iconName = document.getElementById('cat-icon')?.value?.trim() || '📁';
    const catType  = document.querySelector('input[name="cat-type"]:checked')?.value || 'EXPENSE';
    if (!name) { showFieldError('cat-name', 'Name is required.'); return; }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/categories/${encodeURIComponent(id)}`, {
        name,
        parentId: parent ? Number(parent) : null,
        nature,
        colorHex: color || undefined,
        type: catType,
        iconName,
      });
      if (res.success) { closeModal(); await warmCache(); renderCategories(); showToast('Category updated!'); }
      else { showToast('Failed to update: ' + (res.error||''), 'error'); }
    });
  }, 'Save Changes');
};

window.deleteCategory = function (id, name) {
  confirmDelete(`Delete category "${name}"? Transactions using this category will become uncategorised.`, async () => {
    try {
      const res = await apiWrite('DELETE', `/api/categories/${encodeURIComponent(id)}`);
      closeModal();
      if (res.success) { await warmCache(); renderCategories(); showToast('Category deleted.'); }
      else { showToast('Failed to delete category.', 'error'); }
    } catch (e) { if (e.message !== 'Session expired') showToast('Error deleting.', 'error'); }
  });
};

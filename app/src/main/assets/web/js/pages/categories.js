/* PAGE: Categories */

async function renderCategories(searchQuery) {
  showSpinner();
  try {
    const json = await apiFetch('/api/categories');
    if (!json.success) { showError('Failed to load categories.'); return; }
    cachedCategories = json.data || [];
    const q = (searchQuery || '').toLowerCase().trim();

    const roots    = cachedCategories.filter(c => !c.parentId);
    const children = cachedCategories.filter(c =>  c.parentId);

    let html = `<div class="page-header">
      <h2 class="section-title">Categories</h2>
      <button class="btn btn-primary btn-sm" onclick="openAddCategory()">+ Add</button>
    </div>
    <div class="search-bar-wrap" style="margin-bottom:16px">
      <input type="text" class="search-bar" id="cat-search" placeholder="Search categories…"
        value="${esc(q)}"
        oninput="renderCategories(this.value)">
    </div>`;

    if (!cachedCategories.length) {
      html += emptyState('', 'No categories found.', '+ Add Category', 'openAddCategory()');
      setContent(html);
      return;
    }

    // Filter roots and their children
    const visibleRoots = roots.filter(r => {
      if (!q) return true;
      if (r.name.toLowerCase().includes(q)) return true;
      return children.some(c => c.parentId === r.id && c.name.toLowerCase().includes(q));
    });

    if (!visibleRoots.length && q) {
      html += `<div style="text-align:center;padding:32px;color:var(--text-muted)">No categories match "<strong>${esc(q)}</strong>"</div>`;
      setContent(html);
      document.getElementById('cat-search')?.focus();
      return;
    }

    visibleRoots.forEach((root, idx) => {
      const rootColor = resolveColor(root.colorHex);
      const subs = children.filter(c => c.parentId === root.id && (!q || c.name.toLowerCase().includes(q) || root.name.toLowerCase().includes(q)));
      const collapseId = `cat-grp-${root.id}`;
      const upBtn   = idx > 0
        ? `<button class="btn-icon" title="Move up" onclick="event.stopPropagation();moveCategoryUp(${root.id})" style="font-size:0.8rem;margin-right:2px">↑</button>` : '';
      const downBtn = idx < visibleRoots.length - 1
        ? `<button class="btn-icon" title="Move down" onclick="event.stopPropagation();moveCategoryDown(${root.id})" style="font-size:0.8rem;margin-right:2px">↓</button>` : '';
      const deleteBtn = !root.isDefault
        ? `<button class="btn-icon" title="Delete" onclick="event.stopPropagation();deleteCategory('${esc(root.id)}','${esc(root.name)}')">✕</button>` : '';

      html += `<div class="cat-group card" style="margin-bottom:10px;overflow:hidden">
        <div class="cat-group-header" onclick="toggleCatGroup('${collapseId}')" style="display:flex;align-items:center;padding:12px 16px;cursor:pointer;gap:10px;border-bottom:1px solid var(--border)">
          <span class="cat-icon-dot" style="background:${rootColor};color:#fff;min-width:32px;height:32px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:0.85rem">${esc(root.name.charAt(0).toUpperCase())}</span>
          <span style="flex:1;font-weight:600;font-size:0.95rem">${esc(root.name)}</span>
          ${root.nature ? `<span class="badge ${natureCls(root.nature)}">${esc(root.nature)}</span>` : ''}
          <span style="font-size:0.78rem;color:var(--text-muted);margin-right:4px">${subs.length} sub</span>
          ${upBtn}${downBtn}
          <button class="btn-icon" title="Edit" onclick="event.stopPropagation();openEditCategory('${esc(root.id)}')">Edit</button>
          ${deleteBtn}
          <span class="cat-chevron" id="${collapseId}-chev" style="font-size:0.75rem;color:var(--text-muted);transition:transform 0.2s">▾</span>
        </div>
        <div id="${collapseId}" class="cat-group-body" style="${q ? '' : ''}">`;

      if (!subs.length) {
        html += `<div style="padding:10px 16px 10px 52px;color:var(--text-muted);font-size:0.82rem;font-style:italic">No subcategories</div>`;
      } else {
        subs.forEach((sub, sidx) => {
          const subColor = resolveColor(sub.colorHex);
          const delSub = !sub.isDefault
            ? `<button class="btn-icon" title="Delete" onclick="deleteCategory('${esc(sub.id)}','${esc(sub.name)}')">✕</button>` : '';
          const mark = q && sub.name.toLowerCase().includes(q) ? `style="background:color-mix(in srgb,var(--accent) 6%,transparent)"` : '';
          html += `<div class="cat-sub-row" ${mark} style="display:flex;align-items:center;padding:9px 16px 9px 52px;${sidx < subs.length - 1 ? 'border-bottom:1px solid var(--border)' : ''}">
            <span style="width:8px;height:8px;border-radius:50%;background:${subColor};margin-right:10px;flex-shrink:0"></span>
            <span style="flex:1;font-size:0.9rem">${esc(sub.name)}</span>
            ${sub.nature ? `<span class="badge ${natureCls(sub.nature)}" style="margin-right:6px">${esc(sub.nature)}</span>` : ''}
            ${sub.isDefault ? '<span style="font-size:0.72rem;color:var(--text-muted);margin-right:6px">Default</span>' : ''}
            <button class="btn-icon" title="Edit" onclick="openEditCategory('${esc(sub.id)}')">Edit</button>
            ${delSub}
          </div>`;
        });
      }
      html += `</div></div>`;
    });

    setContent(html);

    // Restore focus to search
    const inp = document.getElementById('cat-search');
    if (inp && q) { inp.focus(); inp.setSelectionRange(q.length, q.length); }
  } catch (e) { if (e.message !== 'Session expired') showError('Error loading categories.'); }
}

function resolveColor(colorHex) {
  if (typeof colorHex === 'number') return '#' + ((colorHex >>> 0) & 0xFFFFFF).toString(16).padStart(6,'0');
  return colorHex || '#9e9e9e';
}

function natureCls(n) {
  return n === 'MUST' ? 'badge-must' : n === 'NEED' ? 'badge-need' : 'badge-want';
}

window.toggleCatGroup = function (id) {
  const body = document.getElementById(id);
  const chev = document.getElementById(id + '-chev');
  if (!body) return;
  const hidden = body.style.display === 'none';
  body.style.display = hidden ? '' : 'none';
  if (chev) chev.style.transform = hidden ? '' : 'rotate(-90deg)';
};

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

function buildCatColorForm(currentColor) {
  const presets = ['#1976D2','#2e7d32','#c62828','#f57f17','#6a1b9a','#00838f','#ad1457','#4e342e','#f57c00','#1565c0','#7b1fa2','#00695c','#880e4f','#37474f','#1b5e20','#33691e'];
  const btns = presets.map(c =>
    `<span class="color-swatch cat-color-btn" style="background:${c};width:24px;height:24px;cursor:pointer;border:2px solid ${c === currentColor ? 'var(--accent)' : 'transparent'};border-radius:50%;flex-shrink:0"
      onclick="document.getElementById('cat-color').value='${c}';document.querySelectorAll('.cat-color-btn').forEach(s=>{s.style.borderColor=s.style.background==='${c}'?'var(--accent)':'transparent'});this.style.borderColor='var(--accent)'"></span>`
  ).join('');
  return `<div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:8px">${btns}</div>
    <input type="text" id="cat-color" value="${esc(currentColor)}" placeholder="#1976D2" maxlength="7" style="width:110px">`;
}

window.openAddCategory = function () {
  const currentColor = '#1976D2';
  const body = `
    ${formField('Category Name', '<input type="text" id="cat-name" placeholder="e.g. Groceries" autofocus>')}
    ${formField('Parent (optional)', buildSelect('cat-parent',[{value:'',label:'— Top-level —'}].concat(cachedCategories.map(c=>({value:String(c.id),label:c.name}))),'','— Top-level —',true))}
    <div class="form-field"><label data-tip="MUST=essential · NEED=important · WANT=discretionary">Nature</label>${buildSelect('cat-nature',[{value:'NEED',label:'Need'},{value:'MUST',label:'Must'},{value:'WANT',label:'Want'}],'NEED','— Select —',false)}</div>
    <div class="form-field"><label>Type</label><div class="chip-group">
      <label class="chip-check active"><input type="radio" name="cat-type" value="EXPENSE" checked onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Expense</span></label>
      <label class="chip-check"><input type="radio" name="cat-type" value="INCOME" onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Income</span></label>
    </div></div>
    <div class="form-field"><label>Color</label>${buildCatColorForm(currentColor)}</div>`;
  openModal('Add Category', body, submitAddCategory, 'Save Category');
  modalConfBtn.className = 'btn btn-primary';
};

window.submitAddCategory = async function () {
  clearFieldErrors(modalBody);
  const name      = document.getElementById('cat-name')?.value.trim();
  const parentVal = document.getElementById('cat-parent')?.value || '';
  const nature    = document.getElementById('cat-nature')?.value;
  const color     = document.getElementById('cat-color')?.value.trim();
  const catType   = document.querySelector('input[name="cat-type"]:checked')?.value || 'EXPENSE';
  if (!name) { showFieldError('cat-name', 'Name is required.'); return; }
  const dup = (cachedCategories||[]).find(c =>
    c.name.toLowerCase() === name.toLowerCase() && String(c.parentId||'') === String(parentVal||'')
  );
  if (dup) { showToast(`A category named "${name}" already exists at this level.`, 'info'); }
  await withLoading(modalConfBtn, async () => {
    const res = await apiWrite('POST', '/api/categories', { name, parentId: parentVal || undefined, nature, colorHex: color || undefined, type: catType });
    if (res.success) { closeModal(); await warmCache(); renderCategories(); showToast('Category created!'); }
    else { showToast('Failed to create category: ' + (res.error || ''), 'error'); }
  });
};

window.openEditCategory = function (id) {
  const catId = Number(id);
  const cat   = cachedCategories.find(c => c.id === catId);
  if (!cat) { showToast('Category not found.', 'error'); return; }
  const currentColor = resolveColor(cat.colorHex);
  const editType = cat.type || 'EXPENSE';
  const body = `
    ${formField('Category Name', `<input type="text" id="cat-name" value="${esc(cat.name)}">`)}
    ${formField('Parent (optional)', buildSelect('cat-parent',[{value:'',label:'— Top-level —'}].concat(cachedCategories.filter(c=>c.id!==catId&&c.parentId!==catId).map(c=>({value:String(c.id),label:c.name}))),cat.parentId?String(cat.parentId):'','— Top-level —',true))}
    <div class="form-field"><label data-tip="MUST=essential · NEED=important · WANT=discretionary">Nature</label>${buildSelect('cat-nature',[{value:'NEED',label:'Need'},{value:'MUST',label:'Must'},{value:'WANT',label:'Want'}],cat.nature||'NEED','— Select —',false)}</div>
    <div class="form-field"><label>Type</label><div class="chip-group">
      <label class="chip-check ${editType==='EXPENSE'?'active':''}"><input type="radio" name="cat-type" value="EXPENSE" ${editType==='EXPENSE'?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Expense</span></label>
      <label class="chip-check ${editType==='INCOME'?'active':''}"><input type="radio" name="cat-type" value="INCOME" ${editType==='INCOME'?'checked':''} onchange="this.closest('.chip-group').querySelectorAll('.chip-check').forEach(l=>l.classList.remove('active'));this.closest('label').classList.add('active')"><span>Income</span></label>
    </div></div>
    <div class="form-field"><label>Color</label>${buildCatColorForm(currentColor)}</div>`;
  openModal(`Edit — ${esc(cat.name)}`, body, async () => {
    clearFieldErrors(modalBody);
    const name    = document.getElementById('cat-name')?.value.trim();
    const parent  = document.getElementById('cat-parent')?.value || null;
    const nature  = document.getElementById('cat-nature')?.value;
    const color   = document.getElementById('cat-color')?.value.trim();
    const catType = document.querySelector('input[name="cat-type"]:checked')?.value || 'EXPENSE';
    if (!name) { showFieldError('cat-name', 'Name is required.'); return; }
    await withLoading(modalConfBtn, async () => {
      const res = await apiWrite('PUT', `/api/categories/${encodeURIComponent(id)}`, {
        name, parentId: parent ? Number(parent) : null, nature, colorHex: color || undefined, type: catType,
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

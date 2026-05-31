/* Shared UI components — chip checkboxes, tag chips, custom select, form helpers */

function chipCheckboxes(id, items, selected) {
  return `<div class="chip-group" id="${id}">` +
    items.map(({value, label}) => {
      const checked = selected.includes(value);
      return `<label class="chip-check ${checked?'active':''}"><input type="checkbox" value="${esc(value)}" ${checked?'checked':''} onchange="this.closest('label').classList.toggle('active',this.checked)"><span>${esc(label)}</span></label>`;
    }).join('') + '</div>';
}

function getCheckedValues(groupId) {
  return [...document.querySelectorAll(`#${groupId} input[type=checkbox]:checked`)].map(el => el.value);
}

function buildTagChipInput(id, cachedTagsArr, selectedArr) {
  const chips = (selectedArr || []).map(tag =>
    `<span class="chip-ms-tag" data-tag="${esc(tag)}">${esc(tag)}<span class="chip-ms-remove" onclick="(function(el){const w=el.closest('.chip-ms-wrap');w.removeAttribute('data-removing');el.closest('.chip-ms-tag').remove();document.getElementById('${id}').value=[...w.querySelectorAll('.chip-ms-tag')].map(c=>c.dataset.tag).join(',');})(this)">×</span></span>`
  ).join('');
  const dlOpts = (cachedTagsArr || []).map(t => `<option value="${esc(t)}">`).join('');
  return `<div class="chip-ms-wrap" id="${id}-wrap">${chips}<input class="chip-ms-input" id="${id}-input" placeholder="Add tag…" list="${id}-datalist" autocomplete="off"></div><datalist id="${id}-datalist">${dlOpts}</datalist><input type="hidden" id="${id}" value="${esc((selectedArr||[]).join(','))}">`;
}

function initTagChipInput(id, cachedTagsArr, selectedArr) {
  const container = document.getElementById(id + '-container');
  if (!container) return;
  container.innerHTML = buildTagChipInput(id, cachedTagsArr, selectedArr);
  const wrap = document.getElementById(id + '-wrap');
  const input = document.getElementById(id + '-input');
  const hidden = document.getElementById(id);
  if (!wrap || !input || !hidden) return;
  wrap.addEventListener('click', () => input.focus());
  function addTag(raw) {
    const tag = raw.trim();
    if (!tag) return;
    const existing = [...wrap.querySelectorAll('.chip-ms-tag')].map(c => c.dataset.tag);
    if (existing.includes(tag)) { input.value = ''; return; }
    const chip = document.createElement('span');
    chip.className = 'chip-ms-tag';
    chip.dataset.tag = tag;
    chip.innerHTML = `${esc(tag)}<span class="chip-ms-remove">×</span>`;
    chip.querySelector('.chip-ms-remove').addEventListener('click', () => {
      chip.remove();
      hidden.value = [...wrap.querySelectorAll('.chip-ms-tag')].map(c => c.dataset.tag).join(',');
    });
    wrap.insertBefore(chip, input);
    input.value = '';
    hidden.value = [...wrap.querySelectorAll('.chip-ms-tag')].map(c => c.dataset.tag).join(',');
  }
  input.addEventListener('keydown', e => {
    if ((e.key === 'Enter' || e.key === ',') && input.value.replace(',','').trim()) {
      e.preventDefault();
      addTag(input.value.replace(/,/g,''));
    }
  });
  input.addEventListener('change', e => {
    if (input.value.trim()) addTag(input.value);
  });
}

function getTagValue(id) {
  const el = document.getElementById(id);
  return el ? el.value : '';
}

function wireDateRange(fromId, toId) {
  const validate = () => {
    const f = document.getElementById(fromId), t = document.getElementById(toId);
    if (f && t && f.value && t.value && f.value > t.value) showFieldError(toId, 'End must be after start.');
    else { const el = document.getElementById(toId); if(el) { el.classList.remove('field-error'); const msg=el.nextElementSibling; if(msg&&msg.classList.contains('field-error-msg')) msg.remove(); } }
  };
  const f = document.getElementById(fromId), t = document.getElementById(toId);
  if (f) f.addEventListener('change', validate);
  if (t) t.addEventListener('change', validate);
}

function buildSelect(id, options, currentValue, placeholder, searchable, onchangeAttr) {
  placeholder = placeholder || '— Select —';
  const selOpt = options.find(o => String(o.value) === String(currentValue));
  const displayLabel = selOpt ? selOpt.label : placeholder;
  const hiddenOpts = options.map(o =>
    `<option value="${esc(String(o.value))}" ${String(o.value)===String(currentValue)?'selected':''}>${esc(o.label)}</option>`
  ).join('');
  const visibleOpts = `<div class="cs-option" data-value="" onclick="csSelect(this,'${id}')">${esc(placeholder)}</div>` +
    options.map(o =>
      `<div class="cs-option${String(o.value)===String(currentValue)?' selected':''}" data-value="${esc(String(o.value))}" onclick="csSelect(this,'${id}')">${esc(o.label)}</div>`
    ).join('');
  return `<div class="custom-select" id="${id}-cs">
  <select id="${id}" style="display:none"${onchangeAttr ? ` onchange="${onchangeAttr}"` : ''}>${hiddenOpts}</select>
  <div class="cs-trigger" tabindex="0" onclick="csToggle(event,'${id}')" onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();csToggle(event,'${id}')}">
    <span class="cs-label" id="${id}-lbl">${esc(displayLabel)}</span>
    <span class="cs-chevron">▾</span>
  </div>
  <div class="cs-dropdown" id="${id}-dd" style="display:none">
    ${searchable ? `<div class="cs-search-wrap"><input type="text" placeholder="Search…" onclick="event.stopPropagation()" oninput="csFilter(this,'${id}')"></div>` : ''}
    <div id="${id}-opts">${visibleOpts}</div>
  </div>
</div>`;
}

window.csToggle = function(e, id) {
  e.stopPropagation();
  const dd = document.getElementById(id + '-dd');
  if (!dd) return;
  const isOpen = dd.style.display !== 'none';
  document.querySelectorAll('.cs-dropdown').forEach(d => { d.style.display = 'none'; });
  document.querySelectorAll('.custom-select').forEach(c => c.classList.remove('open'));
  if (!isOpen) {
    dd.style.display = '';
    const cs = document.getElementById(id + '-cs');
    if (cs) cs.classList.add('open');
    const search = dd.querySelector('.cs-search-wrap input');
    if (search) search.focus();
  }
};

window.csSelect = function(optEl, id) {
  const value = optEl.dataset.value;
  const label = optEl.textContent.trim();
  const sel = document.getElementById(id);
  const lbl = document.getElementById(id + '-lbl');
  const cs  = document.getElementById(id + '-cs');
  const dd  = document.getElementById(id + '-dd');
  if (sel) sel.value = value;
  if (lbl) lbl.textContent = label;
  if (cs) { cs.querySelectorAll('.cs-option').forEach(o => o.classList.remove('selected')); optEl.classList.add('selected'); cs.classList.remove('open'); }
  if (dd) dd.style.display = 'none';
  if (sel) sel.dispatchEvent(new Event('change', { bubbles: true }));
};

window.csFilter = function(input, id) {
  const q = input.value.toLowerCase();
  const opts = document.querySelectorAll(`#${id}-opts .cs-option`);
  opts.forEach(o => { o.style.display = o.textContent.toLowerCase().includes(q) ? '' : 'none'; });
};

document.addEventListener('click', () => {
  document.querySelectorAll('.cs-dropdown').forEach(d => { d.style.display = 'none'; });
  document.querySelectorAll('.custom-select').forEach(c => c.classList.remove('open'));
});

function emptyState(icon, message, actionLabel, onAction) {
  return `<div class="empty-state">
    <div style="font-size:2.5rem;margin-bottom:8px">${icon}</div>
    <div style="font-weight:600;margin-bottom:4px">${esc(message)}</div>
    ${actionLabel ? `<button class="btn btn-primary btn-sm" onclick="${onAction}" style="margin-top:12px">${esc(actionLabel)}</button>` : ''}
  </div>`;
}

function formField(label, inputHtml) {
  return `<div class="form-field"><label>${esc(label)}</label>${inputHtml}</div>`;
}
function formRow(...fields) {
  return `<div class="form-row">${fields.join('')}</div>`;
}

window.selectColorChip = function (hiddenId, rowId, chip) {
  document.querySelectorAll(`#${rowId} .color-chip`).forEach(c => c.classList.remove('selected'));
  chip.classList.add('selected');
  document.getElementById(hiddenId).value = chip.dataset.color;
};

function catOptions(includeBlank) {
  let o = includeBlank ? '<option value="">— None —</option>' : '';
  cachedCategories.forEach(c => { o += `<option value="${esc(c.id)}">${esc(c.name)}</option>`; });
  return o;
}
function accOptions() {
  let o = '<option value="">— Select account —</option>';
  cachedAccounts.forEach(a => { o += `<option value="${esc(a.id)}">${esc(a.name)} (${esc(a.currency)})</option>`; });
  return o;
}

window.navPage = function (page) {
  const params = new URLSearchParams(location.hash.split('?')[1] || '');
  params.set('page', page);
  location.hash = '#/records?' + params.toString();
};

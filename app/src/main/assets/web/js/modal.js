/* Modal system — cached refs + open/close/confirm/info helpers */

const modalOverlay = document.getElementById('modal-overlay');
const modalTitle   = document.getElementById('modal-title');
const modalBody    = document.getElementById('modal-body');
const modalConfBtn = document.getElementById('modal-confirm-btn');

function openModal(titleHtml, bodyHtml, onConfirm, confirmLabel) {
  modalTitle.innerHTML = titleHtml;
  modalBody.innerHTML  = bodyHtml;
  modalConfBtn.className   = 'btn btn-primary';
  modalConfBtn.disabled    = false;
  modalConfBtn.style.display = (confirmLabel === null) ? 'none' : '';
  modalConfBtn.textContent = confirmLabel || 'Confirm';
  modalConfBtn.onclick = () => { if (onConfirm) onConfirm(); };
  modalOverlay.classList.add('open');
  document.body.classList.add('modal-open');
  setTimeout(() => {
    const first = modalBody.querySelector('input:not([type=hidden]):not([disabled]), select, textarea');
    if (first) first.focus();
  }, 60);
  const modalBox = document.getElementById('modal-box');
  if (modalBox) {
    const handler = e => {
      if (e.key === 'Enter' && !e.shiftKey && !modalConfBtn.disabled && modalConfBtn.style.display !== 'none' && document.activeElement?.tagName !== 'TEXTAREA') {
        e.preventDefault();
        modalConfBtn.click();
      }
    };
    if (modalBox._enterHandler) modalBox.removeEventListener('keydown', modalBox._enterHandler);
    modalBox.addEventListener('keydown', handler);
    modalBox._enterHandler = handler;
  }
}

function closeModal() {
  modalOverlay.classList.remove('open');
  document.body.classList.remove('modal-open');
  modalConfBtn.onclick = null;
  modalConfBtn.style.display = '';
  modalConfBtn.disabled = false;
  modalConfBtn.className = 'btn btn-primary';
  const modalBox = document.getElementById('modal-box');
  if (modalBox && modalBox._enterHandler) {
    modalBox.removeEventListener('keydown', modalBox._enterHandler);
    delete modalBox._enterHandler;
  }
}

function confirmDelete(message, onConfirm) {
  openModal(
    '<span style="color:var(--danger)">⚠️ Confirm Delete</span>',
    `<p>${esc(message)}</p>`,
    onConfirm,
    'Delete'
  );
  modalConfBtn.className = 'btn btn-danger';
}

function infoModal(titleHtml, bodyHtml) {
  openModal(titleHtml, bodyHtml, null, 'OK');
  modalConfBtn.onclick = closeModal;
}

modalOverlay.addEventListener('click', e => { if (e.target === modalOverlay) closeModal(); });

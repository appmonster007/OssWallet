/* HTTP helpers — depend on state.js (token, baseUrl) and ui.js (showToast, showLogin) */

async function apiFetch(path) {
  const res = await fetch(baseUrl + path, { headers: { 'Authorization': 'Bearer ' + token } });
  if (res.status === 401) { token = ''; sessionStorage.removeItem('oss_token'); showToast('Session expired. Please log in again.', 'info'); showLogin(); throw new Error('Session expired'); }
  return res.json();
}

async function apiWrite(method, path, body) {
  const res = await fetch(baseUrl + path, {
    method,
    headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
    body: body !== undefined ? JSON.stringify(body) : undefined
  });
  if (res.status === 401) { token = ''; sessionStorage.removeItem('oss_token'); showToast('Session expired. Please log in again.', 'info'); showLogin(); throw new Error('Session expired'); }
  try { return await res.json(); } catch { return { success: res.ok }; }
}

/** Sends a file as the raw request body (the server reads the name from X-File-Name). */
async function apiUpload(path, file) {
  const res = await fetch(baseUrl + path, {
    method: 'POST',
    headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/octet-stream', 'X-File-Name': encodeURIComponent(file.name) },
    body: file
  });
  if (res.status === 401) { token = ''; sessionStorage.removeItem('oss_token'); showToast('Session expired. Please log in again.', 'info'); showLogin(); throw new Error('Session expired'); }
  try { return await res.json(); } catch { return { success: res.ok, error: 'Upload failed' }; }
}

/** Fetches a protected file; returns null when it can't be read. */
async function apiBlob(path) {
  const res = await fetch(baseUrl + path, { headers: { 'Authorization': 'Bearer ' + token } });
  if (res.status === 401) { token = ''; sessionStorage.removeItem('oss_token'); showToast('Session expired. Please log in again.', 'info'); showLogin(); throw new Error('Session expired'); }
  return res.ok ? res.blob() : null;
}

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

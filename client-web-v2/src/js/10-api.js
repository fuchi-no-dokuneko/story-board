async function request(path, options = {}) {
  const url = window.StoryBlockAuth.resolveSameOriginUrl(path, location.origin);
  const response = await fetch(url, {...options, headers: {
    Accept: 'application/json', ...window.StoryBlockAuth.authorizationHeaders(S.token),
    ...(options.headers || {}),
  }});
  if (!response.ok) {
    let detail = `HTTP ${response.status}`;
    try { const error = await response.json(); detail = error.detail || error.title || detail; } catch {}
    const error = new Error(detail); error.status = response.status; throw error;
  }
  return response;
}
const json = async (path, options) => (await request(path, options)).json();
function revisionPost(revision, body) {
  return {method:'POST', headers:{'Content-Type':'application/json',
    'If-Match':`"${revision.content_hash}"`, 'Idempotency-Key':crypto.randomUUID()},
    body:JSON.stringify({revision_id:revision.revision_id,...body})};
}
function report(error, id = 'app-message') {
  if (error.name === 'AbortError') return;
  const target = $(id);
  if (target) { target.hidden = false; target.textContent = error.message; }
  if (error.status === 401 || error.status === 403) {
    $('connection-hint').hidden = false;
    $('connection-hint').textContent = t('accessNeeded');
  }
}
async function health() {
  try { S.online = (await json('/actuator/health')).status === 'UP'; }
  catch { S.online = false; }
  const target = $('status-text');
  if (target) { target.textContent = t(S.online ? 'online' : 'offline');
    $('service-pill').dataset.state = S.online ? 'up' : 'down'; }
}
function downloadBlob(blob, name) {
  const url = URL.createObjectURL(blob), link = document.createElement('a');
  link.href = url; link.download = name.replace(/[\\/:*?"<>|]/g, '_');
  document.body.append(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}

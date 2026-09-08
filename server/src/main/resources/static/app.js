const { authorizationHeaders, resolveSameOriginUrl } = window.StoryBlockAuth;

const state = {
  page: 0,
  size: 25,
  totalPages: 0,
  query: '',
  selectedNovelId: null,
  operatorToken: '',
  authRevision: 0,
  catalogRequestId: 0,
};

const elements = Object.fromEntries([
  'status-dot', 'status-text', 'library-tab', 'console-tab', 'library-view', 'console-view',
  'catalog-search', 'catalog-refresh', 'catalog-total', 'catalog-loading', 'catalog-empty',
  'novel-list', 'page-previous', 'page-next', 'page-label', 'reader-empty', 'reader-loading',
  'reader-content', 'reader-language', 'reader-title', 'reader-id', 'reader-registration',
  'stat-han', 'stat-chapters', 'stat-scenes', 'stat-blocks', 'stat-zombies', 'stat-cannons',
  'character-list', 'chapter-nav', 'chapter-content', 'meta-revision', 'meta-updated',
  'meta-hash', 'meta-han-hash', 'operator-form', 'operator-token', 'operator-connect',
  'operator-clear', 'operator-status', 'method', 'path', 'send',
  'response', 'response-status',
].map((id) => [id, document.getElementById(id)]));

function text(value, fallback = 'Not recorded') {
  if (value === null || value === undefined || value === '') return fallback;
  return String(value);
}

function number(value) {
  return Number.isFinite(value) ? new Intl.NumberFormat().format(value) : '0';
}

function show(element, visible) {
  element.hidden = !visible;
}

function setServiceStatus(kind, label) {
  elements['status-dot'].className = kind;
  elements['status-text'].textContent = label;
}

function setOperatorStatus(kind, label) {
  elements['operator-status'].dataset.state = kind;
  elements['operator-status'].textContent = label;
}

function reportAuthenticationError(error) {
  if (error.status === 401) {
    setOperatorStatus(
      'error',
      state.operatorToken ? 'Token rejected; paste a valid owner token' : 'Owner token required',
    );
    return true;
  }
  if (error.status === 403) {
    setOperatorStatus('error', 'Token lacks operator access');
    return true;
  }
  return false;
}

async function fetchJson(url, options = {}) {
  const target = resolveSameOriginUrl(url, location.origin);
  const response = await fetch(target, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...authorizationHeaders(state.operatorToken),
      ...(options.headers || {}),
    },
  });
  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    try {
      const problem = await response.json();
      message = problem.detail || problem.title || message;
    } catch {
      // Keep the status text when the response is not JSON.
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return response.json();
}

function switchView(view) {
  const libraryActive = view === 'library';
  elements['library-tab'].classList.toggle('active', libraryActive);
  elements['console-tab'].classList.toggle('active', !libraryActive);
  elements['library-tab'].setAttribute('aria-selected', String(libraryActive));
  elements['console-tab'].setAttribute('aria-selected', String(!libraryActive));
  show(elements['library-view'], libraryActive);
  show(elements['console-view'], !libraryActive);
}

function catalogItem(novel) {
  const item = document.createElement('li');
  const button = document.createElement('button');
  const title = document.createElement('strong');
  const summary = document.createElement('span');
  const id = document.createElement('span');
  button.type = 'button';
  button.className = 'novel-item';
  button.classList.toggle('selected', novel.novel_id === state.selectedNovelId);
  button.dataset.novelId = novel.novel_id;
  button.setAttribute('aria-pressed', String(novel.novel_id === state.selectedNovelId));
  title.textContent = text(novel.title, 'Untitled novel');
  summary.textContent = `${text(novel.language, 'Unknown language')} / ${number(novel.chapter_count)} chapters / ${number(novel.han_character_count)} Han`;
  id.className = 'catalog-id';
  id.textContent = novel.novel_id;
  button.append(title, summary, id);
  button.addEventListener('click', () => selectNovel(novel.novel_id));
  item.append(button);
  return item;
}

function updatePagination(payload) {
  state.totalPages = payload.total_pages;
  const displayedPage = payload.total_pages === 0 ? 0 : payload.page + 1;
  elements['page-label'].textContent = `Page ${displayedPage} of ${payload.total_pages}`;
  elements['page-previous'].disabled = payload.page <= 0;
  elements['page-next'].disabled = payload.page + 1 >= payload.total_pages;
}

async function loadCatalog({ preserveSelection = true } = {}) {
  const requestId = ++state.catalogRequestId;
  show(elements['catalog-loading'], true);
  show(elements['catalog-empty'], false);
  elements['catalog-empty'].textContent = 'No novels found.';
  elements['novel-list'].replaceChildren();
  try {
    const params = new URLSearchParams({
      page: String(state.page),
      size: String(state.size),
      q: state.query,
    });
    const payload = await fetchJson(`/v1/admin/novels?${params}`);
    if (requestId !== state.catalogRequestId) return false;
    show(elements['catalog-loading'], false);
    elements['catalog-total'].textContent = `${number(payload.total)} persisted novel${payload.total === 1 ? '' : 's'}`;
    updatePagination(payload);
    setOperatorStatus(
      'active',
      state.operatorToken ? 'Owner token active' : 'Trusted-LAN access active',
    );
    elements['operator-clear'].disabled = !state.operatorToken;
    if (payload.items.length === 0) {
      show(elements['catalog-empty'], true);
      if (!preserveSelection) clearReader();
      return true;
    }
    payload.items.forEach((novel) => elements['novel-list'].append(catalogItem(novel)));
    if (!preserveSelection || !state.selectedNovelId) {
      await selectNovel(payload.items[0].novel_id);
    } else {
      await selectNovel(state.selectedNovelId);
    }
    return true;
  } catch (error) {
    if (requestId !== state.catalogRequestId) return false;
    show(elements['catalog-loading'], false);
    show(elements['catalog-empty'], true);
    elements['catalog-empty'].textContent = `Catalog unavailable: ${error.message}`;
    elements['catalog-total'].textContent = 'Unable to load catalog';
    updatePagination({ page: 0, total_pages: 0 });
    clearReader();
    if (!reportAuthenticationError(error)) {
      setServiceStatus('down', 'Service unavailable');
      setOperatorStatus('error', 'Could not load protected library data');
    }
    return false;
  }
}

function clearReader() {
  state.selectedNovelId = null;
  show(elements['reader-empty'], true);
  show(elements['reader-loading'], false);
  show(elements['reader-content'], false);
}

function renderCharacters(characters) {
  elements['character-list'].replaceChildren();
  if (!Array.isArray(characters) || characters.length === 0) {
    const item = document.createElement('li');
    item.textContent = 'Not recorded';
    elements['character-list'].append(item);
    return;
  }
  characters.forEach((character) => {
    const item = document.createElement('li');
    item.textContent = character;
    elements['character-list'].append(item);
  });
}

function chapterBlocks(chapter) {
  const scenes = Array.isArray(chapter.scenes) ? chapter.scenes : [];
  return scenes.flatMap((scene) => Array.isArray(scene.blocks) ? scene.blocks : []);
}

function renderChapters(chapters) {
  elements['chapter-nav'].replaceChildren();
  elements['chapter-content'].replaceChildren();
  if (!Array.isArray(chapters) || chapters.length === 0) {
    const empty = document.createElement('p');
    empty.className = 'pane-message';
    empty.textContent = 'This revision has no chapters.';
    elements['chapter-content'].append(empty);
    return;
  }
  chapters.forEach((chapter, index) => {
    const sectionId = `chapter-${index + 1}`;
    const title = text(chapter.title, `Chapter ${index + 1}`);
    const navButton = document.createElement('button');
    navButton.type = 'button';
    navButton.className = 'chapter-link';
    navButton.textContent = `${index + 1}. ${title}`;
    navButton.addEventListener('click', () => document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' }));
    elements['chapter-nav'].append(navButton);

    const section = document.createElement('section');
    const heading = document.createElement('h3');
    section.id = sectionId;
    section.className = 'chapter-section';
    heading.textContent = title;
    section.append(heading);
    chapterBlocks(chapter).forEach((block) => {
      const paragraph = document.createElement('p');
      paragraph.className = 'story-block';
      paragraph.textContent = text(block.text, '');
      section.append(paragraph);
    });
    elements['chapter-content'].append(section);
  });
}

function renderNovel(payload) {
  const novel = payload.novel;
  const revision = payload.revision;
  elements['reader-language'].textContent = text(novel.language, 'Unknown language');
  elements['reader-title'].textContent = text(novel.title, 'Untitled novel');
  elements['reader-id'].textContent = novel.novel_id;
  elements['reader-registration'].textContent = novel.agent_write_registered ? 'Agent write registered' : 'Imported revision';
  elements['reader-registration'].classList.toggle('registered', novel.agent_write_registered);
  elements['stat-han'].textContent = number(novel.han_character_count);
  elements['stat-chapters'].textContent = number(novel.chapter_count);
  elements['stat-scenes'].textContent = number(novel.scene_count);
  elements['stat-blocks'].textContent = number(novel.block_count);
  elements['stat-zombies'].textContent = number(novel.zombie_count);
  elements['stat-cannons'].textContent = number(novel.tnt_cannon_count);
  elements['meta-revision'].textContent = novel.head_revision_id;
  elements['meta-updated'].textContent = novel.updated_at;
  elements['meta-hash'].textContent = novel.head_hash;
  elements['meta-han-hash'].textContent = novel.han_text_sha256;
  renderCharacters(novel.main_characters);
  renderChapters(revision.chapters);
}

async function selectNovel(novelId) {
  const authRevision = state.authRevision;
  state.selectedNovelId = novelId;
  document.querySelectorAll('.novel-item').forEach((button) => {
    const selected = button.dataset.novelId === novelId;
    button.classList.toggle('selected', selected);
    button.setAttribute('aria-pressed', String(selected));
  });
  show(elements['reader-empty'], false);
  show(elements['reader-loading'], true);
  show(elements['reader-content'], false);
  try {
    const payload = await fetchJson(`/v1/admin/novels/${encodeURIComponent(novelId)}`);
    if (state.selectedNovelId !== novelId || authRevision !== state.authRevision) return;
    renderNovel(payload);
    show(elements['reader-loading'], false);
    show(elements['reader-content'], true);
    history.replaceState(null, '', `#${encodeURIComponent(novelId)}`);
  } catch (error) {
    if (state.selectedNovelId !== novelId || authRevision !== state.authRevision) return;
    reportAuthenticationError(error);
    show(elements['reader-loading'], false);
    show(elements['reader-empty'], true);
    elements['reader-empty'].querySelector('h2').textContent = 'Unable to read novel';
    elements['reader-empty'].querySelector('p').textContent = error.message;
  }
}

async function applyOperatorToken(event) {
  event.preventDefault();
  const token = elements['operator-token'].value.trim();
  if (!token) {
    setOperatorStatus('error', 'Paste the owner token first');
    elements['operator-token'].focus();
    return;
  }
  try {
    authorizationHeaders(token);
  } catch (error) {
    setOperatorStatus('error', error.message);
    return;
  }
  state.operatorToken = token;
  state.authRevision += 1;
  elements['operator-token'].value = '';
  elements['operator-clear'].disabled = false;
  setOperatorStatus('checking', 'Checking operator token…');
  state.page = 0;
  await loadCatalog({ preserveSelection: false });
}

async function clearOperatorToken() {
  state.operatorToken = '';
  state.authRevision += 1;
  elements['operator-token'].value = '';
  elements['operator-clear'].disabled = true;
  setOperatorStatus('checking', 'Operator token cleared; checking trusted-LAN access…');
  elements['novel-list'].replaceChildren();
  elements['catalog-total'].textContent = 'Checking access';
  clearReader();
  state.page = 0;
  await loadCatalog({ preserveSelection: false });
}

async function sendConsoleRequest() {
  elements.response.textContent = 'Loading...';
  elements['response-status'].textContent = '';
  try {
    const target = resolveSameOriginUrl(elements.path.value, location.origin);
    const headers = {
      Accept: 'application/json, application/yaml, text/plain',
      ...authorizationHeaders(state.operatorToken),
    };
    const options = { method: elements.method.value, headers };
    const result = await fetch(target, options);
    const responseText = await result.text();
    elements['response-status'].textContent = `${result.status} ${result.statusText}`;
    try {
      elements.response.textContent = JSON.stringify(JSON.parse(responseText), null, 2);
    } catch {
      elements.response.textContent = responseText || '(empty response)';
    }
  } catch (error) {
    elements['response-status'].textContent = error.code
      ? 'Request blocked'
      : 'Network error';
    elements.response.textContent = error.message;
  }
}

let searchTimer;
elements['catalog-search'].addEventListener('input', () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    state.query = elements['catalog-search'].value.trim();
    state.page = 0;
    loadCatalog({ preserveSelection: false });
  }, 220);
});
elements['catalog-refresh'].addEventListener('click', () => loadCatalog());
elements['page-previous'].addEventListener('click', () => {
  if (state.page > 0) {
    state.page -= 1;
    loadCatalog({ preserveSelection: false });
  }
});
elements['page-next'].addEventListener('click', () => {
  if (state.page + 1 < state.totalPages) {
    state.page += 1;
    loadCatalog({ preserveSelection: false });
  }
});
elements['library-tab'].addEventListener('click', () => switchView('library'));
elements['console-tab'].addEventListener('click', () => switchView('console'));
elements['operator-form'].addEventListener('submit', applyOperatorToken);
elements['operator-clear'].addEventListener('click', clearOperatorToken);
elements.send.addEventListener('click', sendConsoleRequest);
document.querySelectorAll('.quick-requests button').forEach((button) => {
  button.addEventListener('click', () => {
    elements.method.value = button.dataset.method;
    elements.path.value = button.dataset.path;
    sendConsoleRequest();
  });
});

fetch('/actuator/health', { headers: { Accept: 'application/json' } })
  .then((response) => response.json())
  .then((health) => {
    const up = health.status === 'UP';
    setServiceStatus(up ? 'up' : 'down', up ? 'Service online' : 'Service degraded');
  })
  .catch(() => setServiceStatus('down', 'Service unavailable'));

state.selectedNovelId = location.hash ? decodeURIComponent(location.hash.slice(1)) : null;
loadCatalog();

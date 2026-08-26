const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const staticDirectory = path.resolve(__dirname, '../../main/resources/static');
const auth = require(path.join(staticDirectory, 'auth.js'));
const appSource = fs.readFileSync(path.join(staticDirectory, 'app.js'), 'utf8');
const htmlSource = fs.readFileSync(path.join(staticDirectory, 'index.html'), 'utf8');

test('the page exposes every referenced control exactly once and loads auth first', () => {
  const htmlIds = [...htmlSource.matchAll(/\bid="([^"]+)"/g)].map((match) => match[1]);
  assert.equal(new Set(htmlIds).size, htmlIds.length, 'HTML IDs must be unique');

  const elementBlock = appSource.match(
    /const elements = Object\.fromEntries\(\[([\s\S]*?)\]\.map/,
  );
  assert.ok(elementBlock, 'Could not find the application element registry');
  const referencedIds = [...elementBlock[1].matchAll(/'([^']+)'/g)].map((match) => match[1]);
  for (const id of referencedIds) {
    assert.ok(htmlIds.includes(id), `Missing HTML element #${id}`);
  }

  assert.match(
    htmlSource,
    /<input id="operator-token" type="password" autocomplete="off"/,
  );
  assert.match(htmlSource, /connect-src 'self'/);
  assert.ok(
    htmlSource.indexOf('src="/auth.js?') < htmlSource.indexOf('src="/app.js?'),
    'auth.js must execute before app.js',
  );
});

test('builds bearer headers without persisting or exposing the token', () => {
  assert.deepEqual(auth.authorizationHeaders('  operator-secret  '), {
    Authorization: 'Bearer operator-secret',
  });
  assert.deepEqual(auth.authorizationHeaders(''), {});
  assert.throws(
    () => auth.authorizationHeaders('secret\nInjected: value'),
    (error) => error.code === 'INVALID_BEARER_TOKEN',
  );
  assert.doesNotMatch(appSource, /\b(?:localStorage|sessionStorage)\b/);
});

test('allows only URLs on the current StoryBlock origin', () => {
  const origin = 'https://192.168.10.20:8443';
  assert.equal(
    auth.resolveSameOriginUrl('/v1/admin/novels?page=1', origin),
    'https://192.168.10.20:8443/v1/admin/novels?page=1',
  );
  assert.equal(
    auth.resolveSameOriginUrl('https://192.168.10.20:8443/actuator/health', origin),
    'https://192.168.10.20:8443/actuator/health',
  );
  for (const target of [
    'https://attacker.example/collect',
    '//attacker.example/collect',
    'https://192.168.10.20:8443@attacker.example/collect',
    'javascript:alert(1)',
  ]) {
    assert.throws(
      () => auth.resolveSameOriginUrl(target, origin),
      (error) => error.code === 'CROSS_ORIGIN_REQUEST',
      target,
    );
  }
});

function fakeElement(id = '') {
  const listeners = new Map();
  const children = [];
  const queryChildren = new Map();
  return {
    id,
    hidden: false,
    value: '',
    textContent: '',
    className: '',
    disabled: false,
    dataset: {},
    attributes: new Map(),
    classList: { toggle() {} },
    addEventListener(type, listener) { listeners.set(type, listener); },
    append(...items) { children.push(...items); },
    replaceChildren(...items) { children.splice(0, children.length, ...items); },
    setAttribute(name, value) { this.attributes.set(name, String(value)); },
    querySelector(selector) {
      if (!queryChildren.has(selector)) queryChildren.set(selector, fakeElement(selector));
      return queryChildren.get(selector);
    },
    focus() { this.focused = true; },
    listeners,
    children,
  };
}

function jsonResponse(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Unauthorized',
    async json() { return body; },
    async text() { return JSON.stringify(body); },
  };
}

async function settlePromises() {
  await new Promise((resolve) => setImmediate(resolve));
  await new Promise((resolve) => setImmediate(resolve));
}

test('the shared token authenticates library and console requests and can be cleared', async () => {
  const elements = new Map();
  const document = {
    getElementById(id) {
      if (!elements.has(id)) elements.set(id, fakeElement(id));
      return elements.get(id);
    },
    createElement() { return fakeElement(); },
    querySelectorAll() { return []; },
  };
  const requests = [];
  const fetch = async (input, options = {}) => {
    const url = String(input);
    requests.push({ url, options });
    if (url === '/actuator/health') return jsonResponse(200, { status: 'UP' });
    if (url.includes('/v1/admin/novels')) {
      if (options.headers?.Authorization === 'Bearer valid-operator-token') {
        if (new URL(url).pathname.endsWith('/nov_test')) {
          return jsonResponse(200, {
            novel: {
              novel_id: 'nov_test',
              title: 'Authenticated novel',
              language: 'en',
              agent_write_registered: true,
              han_character_count: 0,
              chapter_count: 0,
              scene_count: 0,
              block_count: 0,
              zombie_count: 0,
              tnt_cannon_count: 0,
              head_revision_id: 'rev_test',
              updated_at: '2026-08-27T00:00:00Z',
              head_hash: 'sha256:test',
              han_text_sha256: 'sha256:test',
              main_characters: [],
            },
            revision: { chapters: [] },
          });
        }
        return jsonResponse(200, {
          page: 0,
          total_pages: 1,
          total: 1,
          items: [{
            novel_id: 'nov_test',
            title: 'Authenticated novel',
            language: 'en',
            chapter_count: 0,
            han_character_count: 0,
          }],
        });
      }
      return jsonResponse(401, { detail: 'A valid bearer credential is required.' });
    }
    return jsonResponse(404, { detail: 'Not found' });
  };
  const context = {
    window: { StoryBlockAuth: auth },
    document,
    fetch,
    location: { origin: 'https://192.168.10.20:8443', hash: '' },
    history: { replaceState() {} },
    URL,
    URLSearchParams,
    Intl,
    clearTimeout,
    setTimeout,
    encodeURIComponent,
    decodeURIComponent,
  };
  vm.runInNewContext(appSource, context, { filename: 'app.js' });
  await settlePromises();

  const initialAdminRequest = requests.find((request) => request.url.includes('/v1/admin/novels'));
  assert.ok(initialAdminRequest);
  assert.equal(initialAdminRequest.options.headers.Authorization, undefined);
  assert.equal(elements.get('operator-status').textContent, 'Owner token required');
  assert.equal(elements.get('status-text').textContent, 'Service online');

  const beforeAuthentication = requests.length;
  elements.get('operator-token').value = 'valid-operator-token';
  await elements.get('operator-form').listeners.get('submit')({ preventDefault() {} });
  const authorizedAdminRequests = requests.slice(beforeAuthentication).filter(
    (request) => request.url.includes('/v1/admin/novels'),
  );
  assert.equal(authorizedAdminRequests.length, 2, 'catalog and selected novel must both load');
  for (const request of authorizedAdminRequests) {
    assert.equal(request.options.headers.Authorization, 'Bearer valid-operator-token');
  }
  assert.equal(elements.get('operator-token').value, '');
  assert.equal(elements.get('operator-clear').disabled, false);
  assert.equal(elements.get('operator-status').textContent, 'Owner token active');

  const beforeExternalRequest = requests.length;
  elements.get('path').value = 'https://attacker.example/collect';
  await elements.get('send').listeners.get('click')();
  assert.equal(requests.length, beforeExternalRequest);
  assert.equal(elements.get('response-status').textContent, 'Request blocked');
  assert.doesNotMatch(elements.get('response').textContent, /valid-operator-token/);

  elements.get('path').value = '/v1/admin/novels';
  await elements.get('send').listeners.get('click')();
  assert.equal(requests.at(-1).options.headers.Authorization, 'Bearer valid-operator-token');

  await elements.get('operator-clear').listeners.get('click')();
  assert.equal(elements.get('operator-clear').disabled, true);
  assert.equal(requests.at(-1).options.headers.Authorization, undefined);
  assert.equal(elements.get('operator-status').textContent, 'Owner token required');
});

const test = require('node:test');
const assert = require('node:assert/strict');
const { fixture, block, descriptor, settle } = require('./image-reader-fixture.cjs');

test('authenticated image bytes render with safe captions and cleaned-up object URLs', async () => {
  const f = fixture();
  const figure = f.api.render(block, 'test-token');
  await settle();
  const [image, caption, status] = figure.children;
  assert.equal(f.calls[0][0], `/v1/artifacts/${descriptor.artifact_id}`);
  assert.equal(f.calls[0][1].headers.Authorization, 'Bearer test-token');
  assert.equal(image.alt, descriptor.alt_text);
  assert.equal(caption.textContent, block.text);
  assert.equal(image.hidden, false);
  assert.equal(status.hidden, true);
  assert.equal(image.src, 'blob:test-1');
  f.api.clear();
  assert.deepEqual(f.revoked, ['blob:test-1']);
  assert.equal(f.calls[0][1].signal.aborted, true);
});

test('failed image loads keep the caption and can be retried', async () => {
  const f = fixture();
  f.respond(async () => ({ ok: false, status: 404 }));
  const figure = f.api.render(block, '');
  await settle();
  const [image, caption, status, retry] = figure.children;
  assert.equal(caption.textContent, block.text);
  assert.equal(image.hidden, true);
  assert.match(status.textContent, /404/);
  assert.equal(retry.hidden, false);
  f.respond(async () => ({ ok: true, blob: async () => ({ type: 'image/png' }) }));
  await retry.listeners.click();
  assert.equal(image.hidden, false);
  assert.equal(retry.hidden, true);
});

test('switching novels cancels a pending image without creating a stale URL', async () => {
  const f = fixture();
  let finish;
  f.respond(() => new Promise(resolve => { finish = resolve; }));
  const figure = f.api.render(block, '');
  f.api.clear();
  finish({ ok: true, blob: async () => ({ type: 'image/png' }) });
  await settle();
  assert.equal(f.urls.length, 0);
  assert.equal(figure.children[0].hidden, true);
});

test('invalid image references never trigger a request', async () => {
  const f = fixture();
  const invalid = { ...block, extensions: { 'storyblock.image': { ...descriptor, artifact_id: 'https://external.invalid/picture' } } };
  const figure = f.api.render(invalid, 'test-token');
  await settle();
  assert.equal(f.calls.length, 0);
  assert.equal(figure.children[0].hidden, true);
  assert.match(figure.children[2].textContent, /Invalid image/);
});

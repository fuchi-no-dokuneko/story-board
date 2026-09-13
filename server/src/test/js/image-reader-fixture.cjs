const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const directory = path.resolve(__dirname, '../../main/resources/static');
const source = fs.readFileSync(path.join(directory, 'reader-images.js'), 'utf8');
const descriptor = {
  artifact_id: 'art_018f0f5e-7b4a-7c00-8000-000000000001',
  media_type: 'image/png', width_px: 320, height_px: 200, alt_text: '<safe alt>',
};
const block = { id: 'blk_Ab123', text: '<safe caption>', extensions: { 'storyblock.image': descriptor } };
function fixture() {
  const calls = [], revoked = [], urls = [];
  let responder = async () => ({ ok: true, blob: async () => ({ type: 'image/png' }) });
  const window = { StoryBlockAuth: require(path.join(directory, 'auth.js')), addEventListener() {} };
  vm.runInNewContext(source, {
    window, AbortController,
    fetch: (...args) => { calls.push(args); return responder(...args); },
    URL: { createObjectURL(blob) { urls.push(blob); return `blob:test-${urls.length}`; }, revokeObjectURL(url) { revoked.push(url); } },
    document: { createElement(tag) {
      return { tag, children: [], listeners: {}, hidden: false, dataset: {},
        append(...nodes) { this.children.push(...nodes); },
        setAttribute() {}, addEventListener(name, callback) { this.listeners[name] = callback; },
        decode: async () => {},
      };
    } },
  });
  return { api: window.StoryBlockImages, calls, revoked, urls, respond(fn) { responder = fn; } };
}
const settle = () => new Promise(resolve => setImmediate(resolve));
module.exports = { fixture, block, descriptor, settle };

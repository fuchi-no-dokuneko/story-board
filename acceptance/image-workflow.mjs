import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { cli, id, json, output, save } from './author-cli.mjs';
const source = json('plugin/examples/minimal-novel.json');
source.novel_id = id('nov'); source.created_at = new Date().toISOString();
source.title = 'Image workflow / 圖片流程';
const novel = source.novel_id;
const manuscript = save('manuscript.json', source);
save('registration.json', cli('register', '--source', manuscript));
assert.equal(cli('verify', '--source', manuscript).ok, true);
const live = cli('read', '--novel-id', novel);
const upload = cli('upload-image', '--novel-id', novel, '--file', process.argv[2],
  '--alt-text', 'Browser acceptance evidence');
save('upload.json', upload);
const edit = {
  operation: {
    operation_id: id('op'), idempotency_key: `image-${novel}`, novel_id: novel,
    base_revision_id: live.head.head_revision_id, expected_head_hash: live.head.head_hash,
    type: 'insert_blocks', payload: {
      insertion_point: { scene_id: live.revision.chapters[0].scenes[0].id, position: 'end' },
      blocks: [{ id: id('blk'), text: '瀏覽器已完成 HTTPS 圖片驗證。',
        meta: {}, image: upload.block_image }],
    },
  },
  candidate_revision_id: id('rev'), candidate_created_at: new Date().toISOString(),
};
const request = save('edit.json', edit);
save('preview.json', cli('preview-edit', '--novel-id', novel, '--file', request));
const commit = cli('commit', '--novel-id', novel, '--file', request);
assert.equal(commit.status, 201); save('commit.json', commit);
const replay = cli('commit', '--novel-id', novel, '--file', request);
assert.equal(replay.body.revision_id, commit.body.revision_id);
const render = save('render.json', { revision_id: commit.body.revision_id });
for (const name of ['first', 'second']) {
  const pdf = cli('render-pdf', '--novel-id', novel, '--file', render,
    '--idempotency-key', `${name}-${novel}`, '--output', `${output}/${name}.pdf`, '--force');
  assert.equal(pdf.images, 1); save(`${name}-pdf.json`, pdf);
}
assert.deepEqual(readFileSync(`${output}/first.pdf`), readFileSync(`${output}/second.pdf`));
save('export.json', cli('export', '--novel-id', novel, '--format', 'canonical-package'));
save('result.json', { novel, revision: commit.body.revision_id, image: upload.block_image });
console.log('PASS: registration, image upload, preview, commit replay, deterministic PDF, export');

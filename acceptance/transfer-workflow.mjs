import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { StoryBlockClient } from '../plugin/scripts/lib/api-client.mjs';
import { cli, json, output, save } from './author-cli.mjs';
const result = json(`${output}/result.json`);
const exported = json(`${output}/export.json`);
const job = cli('job', '--job-id', exported.body.job_id);
assert.equal(job.body.status, 'succeeded'); save('job.json', job);
cli('artifact', '--artifact-id', job.body.result_artifact_id,
  '--output', `${output}/package.json`, '--force');
const target = new StoryBlockClient({ baseUrl: process.argv[2] });
const imported = await target.request({ method: 'POST', pathname: '/v1/imports',
  headers: { 'If-Match': '*', 'Idempotency-Key': `transfer-${result.novel}` },
  body: { format: 'canonical-package', document: json(`${output}/package.json`) },
});
assert.equal(imported.data.head_revision_id, result.revision);
save('import.json', imported.data);
const image = await target.request({ pathname: `/v1/artifacts/${result.image.artifact_id}`,
  responseType: 'buffer' });
const source = new StoryBlockClient();
const original = await source.request({ pathname: `/v1/artifacts/${result.image.artifact_id}`,
  responseType: 'buffer' });
assert.deepEqual(image.data, original.data);
await assert.rejects(target.request({ method: 'POST',
  pathname: `/v1/novels/${result.novel}/pdf-renders`,
  headers: { Accept: 'application/json', 'If-Match': `"${imported.data.head_hash}"`,
    'Idempotency-Key': `invalid-accept-${result.novel}` },
  body: { revision_id: result.revision },
}), error => error.status === 406 && error.problem.code === 'MALFORMED_REQUEST');
const pdf = await target.request({ method: 'POST',
  pathname: `/v1/novels/${result.novel}/pdf-renders`, responseType: 'buffer',
  headers: { Accept: 'application/pdf', 'If-Match': `"${imported.data.head_hash}"`,
    'Idempotency-Key': `pdf-${result.novel}` },
  body: { revision_id: result.revision },
});
assert.deepEqual(pdf.data, readFileSync(`${output}/first.pdf`));
console.log('PASS: package import into a separate database, exact image and PDF preservation');

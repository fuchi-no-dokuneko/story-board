import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { readFile } from "node:fs/promises";
import { promisify } from "node:util";
import { test } from "node:test";
import { listDtos } from "../../scripts/lib/dtos.mjs";

const execute = promisify(execFile);

test("generated catalogs are reproducible from bundled manifests", async () => {
  const skillRoot = new URL("../../", import.meta.url);
  const endpointUrl = new URL("references/endpoints.md", skillRoot);
  const dtoUrl = new URL("references/dtos.md", skillRoot);
  const before = await Promise.all([readFile(endpointUrl, "utf8"), readFile(dtoUrl, "utf8")]);
  const index = JSON.parse(await readFile(new URL("references/dtos.json", skillRoot), "utf8"));
  const schemas = await listDtos();
  assert.equal(index.count, schemas.length);
  assert.deepEqual(index.dtos.map(item => item.name).sort(), schemas.map(item => item.name).sort());
  await execute(process.execPath, [new URL("scripts/generate-docs.mjs", skillRoot).pathname]);
  const after = await Promise.all([readFile(endpointUrl, "utf8"), readFile(dtoUrl, "utf8")]);
  assert.deepEqual(after, before);
  assert.match(after[0], /45 code-verified programmatic routes/u);
  assert.match(after[1], /160 standalone JSON Schema/u);
});

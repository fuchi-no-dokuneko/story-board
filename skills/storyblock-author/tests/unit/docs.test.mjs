import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { readFile } from "node:fs/promises";
import { promisify } from "node:util";
import { test } from "node:test";

const execute = promisify(execFile);

test("generated catalogs are reproducible from bundled manifests", async () => {
  const skillRoot = new URL("../../", import.meta.url);
  const endpointUrl = new URL("references/endpoints.md", skillRoot);
  const dtoUrl = new URL("references/dtos.md", skillRoot);
  const before = await Promise.all([readFile(endpointUrl, "utf8"), readFile(dtoUrl, "utf8")]);
  await execute(process.execPath, [new URL("scripts/generate-docs.mjs", skillRoot).pathname]);
  const after = await Promise.all([readFile(endpointUrl, "utf8"), readFile(dtoUrl, "utf8")]);
  assert.deepEqual(after, before);
  assert.match(after[0], /37 code-verified programmatic routes/u);
  assert.match(after[1], /135 standalone JSON Schema/u);
});

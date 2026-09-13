import assert from "node:assert/strict";
import { test } from "node:test";
import { mkdtemp, writeFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { runCli } from "../../scripts/lib/cli.mjs";
import { getSchema } from "../../scripts/lib/dtos.mjs";

async function run(args, transport) {
  let output = "", errors = "";
  const code = await runCli(args, { stdout: s => { output += s; }, stderr: s => { errors += s; },
    clientOverrides: { transport } });
  assert.equal(code, 0, errors);
  return JSON.parse(output);
}
const response = body => ({ status: 200, headers: { "content-type": "application/json" }, body: Buffer.from(JSON.stringify(body)) });

test("block reads pin the revision and preserve a scene-local half-open slice", async () => {
  const value = (await getSchema("BlockSlice")).examples[0];
  const result = await run(["blocks", "--novel-id", value.novel_id, "--revision-id", value.revision_id,
    "--scene-id", value.scene_id, "--start", "0", "--end", "1", "--json"], async request => {
    assert.equal(request.method, "GET");
    assert.equal(request.url.pathname, `/v1/novels/${value.novel_id}/scenes/${value.scene_id}/blocks`);
    assert.equal(request.url.searchParams.get("revision_id"), value.revision_id);
    assert.equal(request.url.searchParams.get("start"), "0");
    assert.equal(request.url.searchParams.get("end"), "1");
    return response(value);
  });
  assert.equal(result.body.blocks[0].text, value.blocks[0].text);
});

test("style comparison waits for one response with a pinned head and extended timeout", async () => {
  const directory = await mkdtemp(join(tmpdir(), "style-cli-"));
  try {
    const file = join(directory, "styles.json");
    await writeFile(file, JSON.stringify({ style_ids: ["lyrical"] }));
    const result = (await getSchema("StyleComparisonResponse")).examples[0];
    const calls = [];
    await run(["compare-styles", "--novel-id", result.novel_id, "--file", file, "--json"], async request => {
      calls.push(request.method);
      assert.equal(request.timeoutMs, 300000);
      if (request.method === "GET") return response({ head_revision_id: result.revision_id, head_hash: result.revision_hash });
      assert.equal(request.headers["If-Match"], `"${result.revision_hash}"`);
      assert.deepEqual(JSON.parse(request.body), { style_ids: ["lyrical"], revision_id: result.revision_id });
      return response(result);
    });
    assert.deepEqual(calls, ["GET", "POST"]);
  } finally { await rm(directory, { recursive: true }); }
});

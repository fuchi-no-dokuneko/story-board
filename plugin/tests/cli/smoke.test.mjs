import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { cp, mkdtemp, readdir, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";
import { test } from "node:test";

const execute = promisify(execFile);

async function removeAssembled(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue;
    const child = join(directory, entry.name);
    if (entry.name.endsWith(".parts")) await rm(child.slice(0, -6), { force: true });
    else await removeAssembled(child);
  }
}

async function command(cwd, args) {
  return execute(process.execPath, ["scripts/storyblock-author.mjs", ...args], {
    cwd,
    env: { PATH: process.env.PATH },
    timeout: 10_000,
    maxBuffer: 32 * 1024 * 1024,
  });
}

test("required offline commands work from a standalone folder copy", async () => {
  const source = fileURLToPath(new URL("../../", import.meta.url));
  const temporary = await mkdtemp(join(tmpdir(), "storyblock-author-standalone-"));
  const copy = join(temporary, "storyblock-author");
  try {
    await cp(source, copy, { recursive: true });
    await removeAssembled(copy);
    await execute(process.execPath, ["scripts/assemble.mjs"], { cwd: copy });

    const endpoints = JSON.parse((await command(copy, ["endpoints", "--json"])).stdout);
    assert.equal(endpoints.endpoints.length, 44);

    const dtos = JSON.parse((await command(copy, ["dtos", "--json"])).stdout);
    assert.equal(dtos.count, 152);

    const validation = JSON.parse((await command(copy, [
      "validate", "--dto", "ApiProblem", "--file", "examples/api-problem.json", "--json",
    ])).stdout);
    assert.equal(validation.valid, true);

    const registration = JSON.parse((await command(copy, ["describe", "agent.novels.register", "--json"])).stdout);
    assert.equal(registration.method, "POST");
    assert.equal(registration.path, "/v1/agent/novels");

    const manuscript = JSON.parse((await command(copy, [
      "validate", "--dto", "AgentNovelRegistrationRequest", "--file", "examples/minimal-novel.json", "--json",
    ])).stdout);
    assert.equal(manuscript.valid, true);

  } finally {
    await rm(temporary, { recursive: true, force: true });
  }
});

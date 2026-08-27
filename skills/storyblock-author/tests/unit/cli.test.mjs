import assert from "node:assert/strict";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";

import { parseCliArgs, runCli } from "../../scripts/lib/cli.mjs";
import { EXIT_CODES } from "../../scripts/lib/problem.mjs";

test("CLI parser separates options and positionals", () => {
  const parsed = parseCliArgs(["describe", "agent.novels.register", "--json", "--timeout-ms=5000"]);
  assert.equal(parsed.command, "describe");
  assert.deepEqual(parsed.positionals, ["agent.novels.register"]);
  assert.equal(parsed.options.get("json"), true);
  assert.equal(parsed.options.get("timeout-ms"), "5000");
  assert.throws(() => parseCliArgs(["endpoints", "--json", "--json"]), /only once/u);
});

test("validation failure emits exit code 3 and performs no network request", async () => {
  const directory = await mkdtemp(join(tmpdir(), "storyblock-author-validation-"));
  try {
    const source = JSON.parse(await readFile(new URL("../../examples/minimal-novel.json", import.meta.url), "utf8"));
    source.expected_han_characters += 1;
    const path = join(directory, "invalid.json");
    await writeFile(path, JSON.stringify(source));
    const stderr = [];
    let networkCalls = 0;
    const exitCode = await runCli([
      "validate", "--dto", "AgentNovelRegistrationRequest", "--file", path, "--json",
    ], {
      stdout: () => {},
      stderr: (value) => stderr.push(value),
      clientOverrides: { transport: async () => { networkCalls += 1; } },
    });
    assert.equal(exitCode, EXIT_CODES.VALIDATION);
    assert.equal(networkCalls, 0);
    const report = JSON.parse(stderr.join(""));
    assert.equal(report.error, "PayloadValidationError");
    assert.match(report.issues[0].message, /chapter text contains 4/u);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("unknown endpoint and invalid usage have distinct exits", async () => {
  assert.equal(await runCli(["describe", "missing.endpoint"], { stdout: () => {}, stderr: () => {} }), EXIT_CODES.UNKNOWN);
  assert.equal(await runCli(["validate"], { stdout: () => {}, stderr: () => {} }), EXIT_CODES.USAGE);
});

test("generic call validates and materializes a normal preview request", async () => {
  const directory = await mkdtemp(join(tmpdir(), "storyblock-author-call-"));
  try {
    const bodyPath = new URL("../../examples/preview-request.json", import.meta.url);
    const body = JSON.parse(await readFile(bodyPath, "utf8"));
    const paramsPath = join(directory, "params.json");
    await writeFile(paramsPath, JSON.stringify({
      path: { novelId: body.operation.novel_id },
      headers: {
        "If-Match": `"${body.operation.expected_head_hash}"`,
        "Idempotency-Key": body.operation.idempotency_key,
      },
    }));
    let captured;
    const stdout = [];
    const exitCode = await runCli([
      "call", "novels.edit-previews.create", "--params", paramsPath,
      "--body", bodyPath.pathname, "--json",
    ], {
      stdout: (value) => stdout.push(value),
      stderr: () => {},
      clientOverrides: {
        transport: async (request) => {
          captured = request;
          return {
            status: 200,
            headers: { "content-type": "application/json" },
            body: Buffer.from("{}"),
          };
        },
      },
    });
    assert.equal(exitCode, EXIT_CODES.SUCCESS);
    assert.equal(captured.method, "POST");
    assert.equal(captured.url.pathname, `/v1/novels/${body.operation.novel_id}/edit-previews`);
    assert.equal(captured.headers["Idempotency-Key"], body.operation.idempotency_key);
    assert.deepEqual(JSON.parse(stdout.join("")), { status: 200, headers: { "content-type": "application/json" }, body: {} });
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("generic call derives Accept from the endpoint response contract", async () => {
  let captured;
  const stdout = [];
  const exitCode = await runCli(["call", "openapi.read", "--json"], {
    stdout: (value) => stdout.push(value),
    stderr: () => {},
    clientOverrides: {
      transport: async (request) => {
        captured = request;
        return {
          status: 200,
          headers: { "content-type": "application/yaml" },
          body: Buffer.from("openapi: 3.1.0\n"),
        };
      },
    },
  });

  assert.equal(exitCode, EXIT_CODES.SUCCESS);
  assert.equal(captured.headers.Accept, "application/yaml, application/problem+json");
  assert.equal(JSON.parse(stdout.join("")).body, "openapi: 3.1.0\n");
});

test("generic call rejects invalid declared headers before network access", async () => {
  const directory = await mkdtemp(join(tmpdir(), "storyblock-author-headers-"));
  try {
    const paramsPath = join(directory, "params.json");
    await writeFile(paramsPath, JSON.stringify({
      headers: { "if-match": "not-a-wildcard", "Idempotency-Key": "header-test" },
    }));
    let networkCalls = 0;
    const stderr = [];
    const exitCode = await runCli([
      "call", "agent.novels.register", "--params", paramsPath,
      "--body", new URL("../../examples/minimal-novel.json", import.meta.url).pathname, "--json",
    ], {
      stdout: () => {},
      stderr: (value) => stderr.push(value),
      clientOverrides: { transport: async () => { networkCalls += 1; } },
    });

    assert.equal(exitCode, EXIT_CODES.VALIDATION);
    assert.equal(networkCalls, 0);
    const report = JSON.parse(stderr.join(""));
    assert.equal(report.dto, "agent.novels.register:If-Match");
    assert.ok(report.issues.some(({ message }) => message.includes("must equal")));
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

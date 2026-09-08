import assert from "node:assert/strict";
import { chmod, mkdtemp, readFile, rm, stat, writeFile } from "node:fs/promises";
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

test("artifact force-overwrite restores private permissions and requests binary content", async () => {
  const directory = await mkdtemp(join(tmpdir(), "storyblock-author-artifact-"));
  try {
    const outputPath = join(directory, "artifact.bin");
    await writeFile(outputPath, "old");
    await chmod(outputPath, 0o644);
    let captured;
    const stdout = [];
    const exitCode = await runCli([
      "artifact", "--artifact-id", "art_018f0f5e-7b4a-7c00-8000-000000000001",
      "--output", outputPath, "--force", "--json",
    ], {
      stdout: (value) => stdout.push(value),
      stderr: () => {},
      clientOverrides: {
        transport: async (request) => {
          captured = request;
          return {
            status: 200,
            headers: { "content-type": "application/octet-stream" },
            body: Buffer.from("replacement"),
          };
        },
      },
    });

    assert.equal(exitCode, EXIT_CODES.SUCCESS);
    assert.equal(captured.headers.Accept, "application/octet-stream, application/problem+json");
    assert.equal(await readFile(outputPath, "utf8"), "replacement");
    assert.equal((await stat(outputPath)).mode & 0o777, 0o600);
    assert.equal(JSON.parse(stdout.join("")).bytes, 11);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("upload-image sends raw bytes and emits a validated image block", async () => {
  const directory = await mkdtemp(join(tmpdir(), "storyblock-author-image-"));
  try {
    const imagePath = join(directory, "reference.png");
    const imageBytes = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
    await writeFile(imagePath, imageBytes);
    const novelId = "nov_018f0f5e-7b4a-7c00-8000-000000000001";
    const revisionId = "rev_018f0f5e-7b4a-7c00-8000-000000000001";
    const hash = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    const requests = [];
    const responses = [
      {
        status: 200,
        headers: { "content-type": "application/json" },
        body: Buffer.from(JSON.stringify({
          novel_id: novelId,
          head_revision_id: revisionId,
          head_sequence: 0,
          head_hash: hash,
          schema_version: "example-1",
        })),
      },
      {
        status: 200,
        headers: { "content-type": "application/json" },
        body: Buffer.from(JSON.stringify({
          chapters: [],
          content_hash: hash,
          created_at: "2026-08-27T12:00:00Z",
          novel_id: novelId,
          parent_revision_id: revisionId,
          revision_id: revisionId,
          schema_version: "1.0.0",
        })),
      },
      {
        status: 201,
        headers: { "content-type": "application/json" },
        body: Buffer.from(JSON.stringify({
          schema_version: "image-upload-1.0.0",
          artifact_id: "art_018f0f5e-7b4a-7c00-8000-000000000001",
          artifact_uri: "/v1/artifacts/art_018f0f5e-7b4a-7c00-8000-000000000001",
          content_hash: hash,
          media_type: "image/png",
          width_px: 1024,
          height_px: 1024,
          idempotent_replay: false,
        })),
      },
    ];
    const stdout = [];
    const exitCode = await runCli([
      "upload-image", "--novel-id", novelId, "--file", imagePath,
      "--alt-text", "Plain-background character reference.", "--json",
    ], {
      stdout: (value) => stdout.push(value),
      stderr: () => {},
      clientOverrides: {
        transport: async (request) => {
          requests.push(request);
          return responses.shift();
        },
      },
    });

    assert.equal(exitCode, EXIT_CODES.SUCCESS);
    assert.equal(requests.length, 3);
    assert.equal(requests[2].url.pathname, `/v1/novels/${novelId}/images`);
    assert.equal(requests[2].headers["Content-Type"], "image/png");
    assert.equal(requests[2].headers["If-Match"], `"${hash}"`);
    assert.deepEqual(requests[2].body, imageBytes);
    const report = JSON.parse(stdout.join(""));
    assert.equal(report.source_bytes, imageBytes.length);
    assert.equal(report.block_image.alt_text, "Plain-background character reference.");
    assert.equal(report.block_image.artifact_id, "art_018f0f5e-7b4a-7c00-8000-000000000001");
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("render-pdf writes exact private PDF bytes and reports render metadata", async () => {
  const directory = await mkdtemp(join(tmpdir(), "storyblock-author-pdf-"));
  try {
    const novelId = "nov_018f0f5e-7b4a-7c00-8000-000000000001";
    const revisionId = "rev_018f0f5e-7b4a-7c00-8000-000000000001";
    const hash = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    const requestPath = join(directory, "request.json");
    const outputPath = join(directory, "novel.pdf");
    await writeFile(requestPath, JSON.stringify({ revision_id: revisionId }));
    const pdfBytes = Buffer.from("%PDF-1.4\ncontract-test\n%%EOF\n");
    const requests = [];
    const responses = [
      {
        status: 200,
        headers: { "content-type": "application/json" },
        body: Buffer.from(JSON.stringify({
          chapters: [],
          content_hash: hash,
          created_at: "2026-08-27T12:00:00Z",
          novel_id: novelId,
          parent_revision_id: revisionId,
          revision_id: revisionId,
          schema_version: "1.0.0",
        })),
      },
      {
        status: 200,
        headers: {
          "content-type": "application/pdf",
          "x-pdf-page-count": "7",
          "x-pdf-image-count": "5",
          "x-pdf-renderer-version": "storyblock-pdf-1",
        },
        body: pdfBytes,
      },
    ];
    const stdout = [];
    const exitCode = await runCli([
      "render-pdf", "--novel-id", novelId, "--file", requestPath,
      "--output", outputPath, "--json",
    ], {
      stdout: (value) => stdout.push(value),
      stderr: () => {},
      clientOverrides: {
        transport: async (request) => {
          requests.push(request);
          return responses.shift();
        },
      },
    });

    assert.equal(exitCode, EXIT_CODES.SUCCESS);
    assert.equal(requests[1].url.pathname, `/v1/novels/${novelId}/pdf-renders`);
    assert.equal(requests[1].headers.Accept, "application/pdf, application/problem+json");
    assert.deepEqual(JSON.parse(requests[1].body.toString("utf8")), { revision_id: revisionId });
    assert.deepEqual(await readFile(outputPath), pdfBytes);
    assert.equal((await stat(outputPath)).mode & 0o777, 0o600);
    const report = JSON.parse(stdout.join(""));
    assert.equal(report.bytes, pdfBytes.length);
    assert.equal(report.pages, 7);
    assert.equal(report.images, 5);
    assert.equal(report.renderer_version, "storyblock-pdf-1");
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

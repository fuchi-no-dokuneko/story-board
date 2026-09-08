import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";

import { getEndpoint, loadEndpointManifest } from "../../scripts/lib/endpoints.mjs";
import { getSchema } from "../../scripts/lib/dtos.mjs";

test("endpoint manifest contains every unique code-derived route", async () => {
  const manifest = await loadEndpointManifest();
  assert.equal(manifest.apiVersion, "v1");
  assert.equal(manifest.endpoints.length, 39);
  assert.equal(new Set(manifest.endpoints.map(({ id }) => id)).size, 39);

  for (const endpoint of manifest.endpoints) {
    assert.match(endpoint.id, /^[a-z][a-z0-9.-]+$/u);
    assert.match(endpoint.method, /^(?:GET|POST|DELETE)$/u);
    assert.match(endpoint.path, /^\//u);
    assert.ok(endpoint.summary.length > 0);
    assert.ok(["confirmed-from-code", "inferred-from-tests", "open-question"].includes(endpoint.confidence));
    assert.ok(endpoint.source.length > 0);
    assert.ok(Array.isArray(endpoint.pathParameters));
    assert.ok(Array.isArray(endpoint.queryParameters));
    assert.ok(Array.isArray(endpoint.requestHeaders));
    assert.ok(Array.isArray(endpoint.responses));
    assert.ok(Array.isArray(endpoint.errors));

    if (endpoint.requestBody?.dto) await getSchema(endpoint.requestBody.dto);
    for (const response of endpoint.responses) if (response.dto) await getSchema(response.dto);
    for (const error of endpoint.errors) if (error.dto) await getSchema(error.dto);
  }
});

test("known code/OpenAPI differences are represented explicitly", async () => {
  assert.equal((await getEndpoint("openapi.read")).path, "/v1/openapi.yaml");
  assert.equal((await getEndpoint("novels.monitor-runs.read")).path, "/v1/novels/{novelId}/monitor-runs/{runId}");
  assert.equal((await getEndpoint("rewrite-proposals.create")).responses[0].dto, "RewriteProposalAccepted");
  assert.equal((await getEndpoint("rewrite-proposals.read")).responses[0].dto, "RewriteProposalResponse");
  assert.match((await getEndpoint("rewrite-proposals.read")).openQuestion, /cross-novel/u);
  assert.equal((await getEndpoint("novels.images.create")).requestBody.binary, true);
  assert.equal((await getEndpoint("novels.pdf-renders.create")).responses[0].dto, "PdfDocument");

  const createStatuses = (await getEndpoint("novels.create")).responses.map(({ status }) => status);
  assert.deepEqual(createStatuses, [200, 201]);
});

test("release metadata agrees on version and zero dependencies", async () => {
  const skillRoot = new URL("../../", import.meta.url);
  const version = (await readFile(new URL("VERSION", skillRoot), "utf8")).trim();
  const packageJson = JSON.parse(await readFile(new URL("package.json", skillRoot), "utf8"));
  const skillJson = JSON.parse(await readFile(new URL("skill.json", skillRoot), "utf8"));
  assert.equal(version, "1.1.0");
  assert.equal(packageJson.version, version);
  assert.equal(skillJson.version, version);
  assert.equal(packageJson.dependencies, undefined);
  assert.deepEqual(skillJson.runtime.dependencies, []);
});

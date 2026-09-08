import assert from "node:assert/strict";
import { test } from "node:test";

import { StoryBlockClient, connectionPolicy } from "../../scripts/lib/api-client.mjs";
import { ApiResponseError, EXIT_CODES, NetworkError, problemReport } from "../../scripts/lib/problem.mjs";

function transportResponse(status, value, headers = {}) {
  return {
    status,
    headers: { "content-type": "application/problem+json", ...headers },
    body: Buffer.from(JSON.stringify(value)),
  };
}

test("connection policy requires HTTPS, accepts self-signed IPv4, and rejects URL credentials", () => {
  assert.deepEqual(connectionPolicy("https://127.0.0.1:8443"), {
    origin: "https://127.0.0.1:8443", rejectUnauthorized: false, family: 4,
  });
  assert.equal(connectionPolicy("https://storyblock.private:9443").rejectUnauthorized, false);
  assert.throws(() => connectionPolicy("http://127.0.0.1:8443"), /must use HTTPS/u);
  assert.throws(() => connectionPolicy("https://user:secret@127.0.0.1:8443"), /must not contain credentials/u);
  assert.throws(() => connectionPolicy("https://[::1]:8443"), /IPv4 endpoints only/u);
});

test("client handles 200 and 201 JSON success and emits bearer headers", async () => {
  for (const status of [200, 201]) {
    let captured;
    const client = new StoryBlockClient({
      accessKey: "nv_key_test",
      transport: async (request) => {
        captured = request;
        return transportResponse(status, { status });
      },
    });
    const response = await client.request({ method: "POST", pathname: "/v1/example", body: { value: true } });
    assert.equal(response.status, status);
    assert.deepEqual(response.data, { status });
    assert.equal(captured.headers.Authorization, "Bearer nv_key_test");
    assert.equal(captured.headers["Content-Type"], "application/json");
    assert.deepEqual(JSON.parse(captured.body.toString("utf8")), { value: true });
  }
});

test("client maps every contracted problem status and preserves extensions", async (context) => {
  for (const status of [400, 401, 403, 404, 409, 422, 500]) {
    await context.test(String(status), async () => {
      const problem = {
        type: `https://storyblock.example/problems/status-${status}`,
        title: `Status ${status}`,
        status,
        code: `STATUS_${status}`,
        detail: `Contract status ${status}`,
        instance: `/v1/status/${status}`,
        violations: [{ code: "EXAMPLE" }],
        request_id: "req_contract_test",
      };
      const client = new StoryBlockClient({
        transport: async () => transportResponse(status, problem, { "retry-after": "60" }),
      });
      await assert.rejects(client.request({ pathname: `/v1/status/${status}` }), (error) => {
        assert.ok(error instanceof ApiResponseError);
        assert.equal(error.exitCode, [401, 403].includes(status) ? EXIT_CODES.AUTH : EXIT_CODES.API);
        const report = problemReport(error);
        assert.equal(report.status, status);
        assert.equal(report.code, `STATUS_${status}`);
        assert.equal(report.request_id, "req_contract_test");
        assert.equal(report.retry_after, "60");
        assert.deepEqual(report.validation_issues, [{ code: "EXAMPLE" }]);
        return true;
      });
    });
  }
});

test("malformed error bodies and transport failures remain diagnosable", async () => {
  const malformed = new StoryBlockClient({
    transport: async () => ({ status: 500, headers: { "x-request-id": "req_header" }, body: Buffer.from("not-json") }),
  });
  await assert.rejects(malformed.request({ pathname: "/v1/fail" }), (error) => {
    assert.equal(error.problem.code, "UNPARSEABLE_ERROR_RESPONSE");
    assert.equal(problemReport(error).request_id, "req_header");
    return true;
  });

  const unreachable = new StoryBlockClient({ transport: async () => { throw new Error("offline"); } });
  await assert.rejects(unreachable.request({ pathname: "/actuator/health" }), (error) => {
    assert.ok(error instanceof NetworkError);
    assert.equal(error.exitCode, EXIT_CODES.NETWORK);
    return true;
  });
});

# StoryBlock endpoint catalog

This catalog contains 37 code-verified programmatic routes. Static browser assets are excluded. Paths are full deployed paths, including `/v1`.

The repository OpenAPI was treated only as an extraction aid. Controller code corrects three known differences: the OpenAPI endpoint itself is added, the monitor status variable is `runId`, and rewrite-proposal response DTOs follow `RewriteProposalController` rather than the published `JobAccepted`/proposal schemas. Actuator routes come from runtime configuration and security tests.

Every `/v1` mutation requires `If-Match` and `Idempotency-Key` in the filter, even when the controller does not consume both values. Every protected `/v1` request is rate-limited per authenticated identity.

## health.read

- Method and path: `GET /actuator/health`
- Purpose: Read aggregate operational health
- Auth: public; scopes: none; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `ActuatorHealthResponse` (application/vnd.spring-boot.actuator.v3+json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/OperationalHealthConfiguration.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/application.properties`.

## health.components.read

- Method and path: `GET /actuator/health/{component}`
- Purpose: Read one authorized health component
- Auth: operator-owner; scopes: none; roles: `operator`.
- Path parameters: `component` (required; type="string", enum=["artifactStorage","backup","migration","sqlite","wal","workerQueue"]).
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `ActuatorHealthResponse` (application/vnd.spring-boot.actuator.v3+json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/OperationalHealthConfiguration.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/test/java/dev/storyblock/api/http/ApiHttpContractTest.java`.

## metrics.list

- Method and path: `GET /actuator/metrics`
- Purpose: List exposed Micrometer metric names
- Auth: operator-owner; scopes: none; roles: `operator`.
- Path parameters: none.
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `ActuatorMetricsListResponse` (application/vnd.spring-boot.actuator.v3+json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/application.properties`, `apps/api/src/test/java/dev/storyblock/api/http/ApiHttpContractTest.java`.

## metrics.read

- Method and path: `GET /actuator/metrics/{metricName}`
- Purpose: Read one exposed Micrometer metric
- Auth: operator-owner; scopes: none; roles: `operator`.
- Path parameters: `metricName` (required; type="string", minLength=1).
- Query parameters: `tag` (optional; type="array", items={"type":"string"}).
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `ActuatorMetricResponse` (application/vnd.spring-boot.actuator.v3+json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/application.properties`.

## access-keys.revoke

- Method and path: `DELETE /v1/access-keys/{keyId}`
- Purpose: Revoke a scoped bearer credential
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:admin`; roles: none.
- Path parameters: `keyId` (required; type="string", pattern="^key_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `RevokeResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/AccessKeyController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## admin.novels.list

- Method and path: `GET /v1/admin/novels`
- Purpose: List persisted novels for the read-only admin library
- Auth: operator-owner; scopes: none; roles: `operator`.
- Path parameters: none.
- Query parameters: `page` (optional; type="integer", minimum=0, default=0); `size` (optional; type="integer", minimum=1, maximum=100, default=25); `q` (optional; type="string", maxLength=200, default="").
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `AdminNovelCatalog` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/AdminNovelController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## admin.novels.read

- Method and path: `GET /v1/admin/novels/{novelId}`
- Purpose: Read a persisted novel and its current canonical revision
- Auth: operator-owner; scopes: none; roles: `operator`.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `AdminNovelDetail` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/AdminNovelController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## agent.novels.register

- Method and path: `POST /v1/agent/novels`
- Purpose: Validate and register a complete agent-authored manuscript
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:admin`; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; const="*"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `AgentNovelRegistrationRequest` (application/json, required).
- Responses: 200 `AgentNovelRegistrationResponse` (application/json); 201 `AgentNovelRegistrationResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/AgentNovelController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## artifacts.download

- Method and path: `GET /v1/artifacts/{artifactId}`
- Purpose: Download a novel-scoped immutable result artifact
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `artifactId` (required; type="string", pattern="^art_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `ArtifactResponse` (application/vnd.storyblock.package+json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/CanonicalTransferController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## imports.create

- Method and path: `POST /v1/imports`
- Purpose: Import a versioned canonical JSON document or package
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:admin`; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; const="*"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `ImportRequest` (application/json, required).
- Responses: 200 `NovelHead` (application/json); 201 `NovelHead` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/CanonicalTransferController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## internal.jobs.claim

- Method and path: `POST /v1/internal/jobs/claims`
- Purpose: Claim one available or expired durable worker lease
- Auth: bearer-or-trusted-lan-owner; scopes: `worker:execute`; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; const="*"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `WorkerClaimRequest` (application/json, required).
- Responses: 200 `WorkerClaimResponse` (application/json); 204 no body.
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleAnalysisController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## internal.jobs.results.submit

- Method and path: `POST /v1/internal/jobs/{jobId}/results`
- Purpose: Submit an idempotent version-bound worker result
- Auth: bearer-or-trusted-lan-owner; scopes: `worker:execute`; roles: none.
- Path parameters: `jobId` (required; type="string", pattern="^job_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `WorkerResultRequest` (application/json, required).
- Responses: 200 `WorkerResultResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleAnalysisController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## jobs.read

- Method and path: `GET /v1/jobs/{jobId}`
- Purpose: Get durable job status within its novel authorization boundary
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `jobId` (required; type="string", pattern="^job_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `Job` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/CanonicalTransferController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.create

- Method and path: `POST /v1/novels`
- Purpose: Create a novel from an initial canonical revision
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:admin`; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; const="*"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `CreateNovelRequest` (application/json, required).
- Responses: 200 `NovelHead` (application/json); 201 `NovelHead` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/NovelController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.head.read

- Method and path: `GET /v1/novels/{novelId}`
- Purpose: Get current novel head metadata
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `NovelHead` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/NovelController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.access-keys.create

- Method and path: `POST /v1/novels/{novelId}/access-keys`
- Purpose: Issue a scoped per-novel bearer credential
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:admin`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `AccessKeyRequest` (application/json, required).
- Responses: 201 `AccessKeyCreated` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/AccessKeyController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.commits.create

- Method and path: `POST /v1/novels/{novelId}/commits`
- Purpose: Commit one validated operation with atomic head CAS
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:commit`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `CommitRequest` (application/json, required).
- Responses: 200 `CommitResponse` (application/json); 201 `CommitResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/CommitController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.detector-runs.create

- Method and path: `POST /v1/novels/{novelId}/detector-runs`
- Purpose: Run the deterministic adjacent metadata detector
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:analyze`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `DetectorRunRequest` (application/json, required).
- Responses: 200 `DetectorRunResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/DetectorController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.edit-previews.create

- Method and path: `POST /v1/novels/{novelId}/edit-previews`
- Purpose: Preview and validate one typed edit operation
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:propose`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `EditPreviewRequest` (application/json, required).
- Responses: 200 `PreviewResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/PreviewController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.exports.create

- Method and path: `POST /v1/novels/{novelId}/exports`
- Purpose: Submit a canonical JSON or package export job
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `ExportRequest` (application/json, required).
- Responses: 202 `JobAccepted` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/CanonicalTransferController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.monitor-packets.create

- Method and path: `POST /v1/novels/{novelId}/monitor-packets`
- Purpose: Resolve a bounded read-only packet for one monitor target
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `MonitorPacketRequest` (application/json, required).
- Responses: 200 `MonitorPacket` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/MonitorController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.monitor-runs.create

- Method and path: `POST /v1/novels/{novelId}/monitor-runs`
- Purpose: Persist an evidence-bound finding or inert proposed operation
- Auth: bearer-or-trusted-lan-owner; scopes: `monitor:submit`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `MonitorSubmissionRequest` (application/json, required).
- Responses: 200 `MonitorSubmissionResponse` (application/json); 201 `MonitorSubmissionResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/MonitorController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.monitor-runs.read

- Method and path: `GET /v1/novels/{novelId}/monitor-runs/{runId}`
- Purpose: Get an immutable monitor output with derived stale state
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"); `runId` (required; type="string", pattern="^mrun_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `MonitorRunStatus` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/MonitorController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.renders.create

- Method and path: `POST /v1/novels/{novelId}/renders`
- Purpose: Render a revision range with resolved metadata
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `RenderRequest` (application/json, required).
- Responses: 200 `RenderPacket` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/RenderController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.revisions.read

- Method and path: `GET /v1/novels/{novelId}/revisions/{revisionId}`
- Purpose: Get an immutable canonical revision
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"); `revisionId` (required; type="string", pattern="^rev_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `CanonicalRevision` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/NovelController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.style-analyses.create

- Method and path: `POST /v1/novels/{novelId}/style-analyses`
- Purpose: Submit a durable style analysis job
- Auth: bearer-or-trusted-lan-owner; scopes: `style:analyze`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `StyleAnalysisRequest` (application/json, required).
- Responses: 202 `StyleAnalysisAccepted` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleAnalysisController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## novels.undo-previews.create

- Method and path: `POST /v1/novels/{novelId}/undo-previews`
- Purpose: Preview restoration of historical canonical content
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:propose`; roles: none.
- Path parameters: `novelId` (required; type="string", pattern="^nov_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `UndoPreviewRequest` (application/json, required).
- Responses: 200 `PreviewResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/PreviewController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## openapi.read

- Method and path: `GET /v1/openapi.yaml`
- Purpose: Read the bundled StoryBlock v1 OpenAPI document
- Auth: public; scopes: none; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `OpenApiDocument` (application/yaml).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/OpenApiDocumentController.java`, `apps/api/src/test/java/dev/storyblock/api/http/ApiHttpContractTest.java`.

## rewrite-proposals.create

- Method and path: `POST /v1/rewrite-proposals`
- Purpose: Submit a durable LLM rewrite proposal job
- Auth: bearer-or-trusted-lan-owner; scopes: `rewrite:propose`; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `RewriteProposalRequest` (application/json, required).
- Responses: 202 `RewriteProposalAccepted` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/RewriteProposalController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## rewrite-proposals.read

- Method and path: `GET /v1/rewrite-proposals/{proposalId}`
- Purpose: Get an immutable rewrite proposal and its validation results
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `proposalId` (required; type="string", pattern="^prp_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `RewriteProposalResponse` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Authorization note: Requires novel:read, but the controller/filter code does not bind proposalId back to the credential novel.
- Confidence: `confirmed-from-code`.
- OPEN QUESTION: Whether cross-novel rewrite proposal reads are intentionally visible to any novel:read credential.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/RewriteProposalController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## style-analyses.read

- Method and path: `GET /v1/style-analyses/{analysisId}`
- Purpose: Get durable style analysis state and result metadata
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `analysisId` (required; type="string", pattern="^ana_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `StyleAnalysisView` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleAnalysisController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## style-analyses.windows.list

- Method and path: `GET /v1/style-analyses/{analysisId}/windows`
- Purpose: Page canonical window findings for a completed style analysis
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `analysisId` (required; type="string", pattern="^ana_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: `cursor` (optional; type="string", minLength=1, maxLength=256); `limit` (optional; type="integer", minimum=1, maximum=200, default=50).
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `StyleAnalysisWindowPage` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleAnalysisController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## style-profiles.create

- Method and path: `POST /v1/style-profiles`
- Purpose: Create an immutable novel-scoped style profile
- Auth: bearer-or-trusted-lan-owner; scopes: `style:admin`; roles: none.
- Path parameters: none.
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; const="*"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `StyleProfileRequest` (application/json, required).
- Responses: 200 `StyleProfile` (application/json); 201 `StyleProfile` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleProfileController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## style-profiles.read

- Method and path: `GET /v1/style-profiles/{profileId}`
- Purpose: Get an immutable style profile within its novel boundary
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `profileId` (required; type="string", pattern="^spf_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `StyleProfile` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleProfileController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## style-profiles.versions.create

- Method and path: `POST /v1/style-profiles/{profileId}/versions`
- Purpose: Create an immutable DRAFT profile baseline version
- Auth: bearer-or-trusted-lan-owner; scopes: `style:admin`; roles: none.
- Path parameters: `profileId` (required; type="string", pattern="^spf_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `StyleProfileVersionContent` (application/json, required).
- Responses: 200 `StyleProfileVersionView` (application/json); 201 `StyleProfileVersionView` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleProfileController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## style-profiles.versions.read

- Method and path: `GET /v1/style-profiles/{profileId}/versions/{versionId}`
- Purpose: Get an immutable profile version and append-only lifecycle
- Auth: bearer-or-trusted-lan-owner; scopes: `novel:read`; roles: none.
- Path parameters: `profileId` (required; type="string", pattern="^spf_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"); `versionId` (required; type="string", pattern="^spv_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: none.
- Responses: 200 `StyleProfileVersionView` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleProfileController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.

## style-profiles.versions.transition

- Method and path: `POST /v1/style-profiles/{profileId}/versions/{versionId}/transitions`
- Purpose: Append an explicit audited profile lifecycle transition
- Auth: bearer-or-trusted-lan-owner; scopes: `style:admin`; roles: none.
- Path parameters: `profileId` (required; type="string", pattern="^spf_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"); `versionId` (required; type="string", pattern="^spv_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").
- Query parameters: none.
- Request headers: `Authorization` (optional; type="string", writeOnly=true); `If-Match` (required; type="string", pattern="^\"sha256:[0-9a-f]{64}\"$"); `Idempotency-Key` (required; type="string", minLength=1, maxLength=200); `X-Request-Id` (optional; type="string", pattern="^[A-Za-z0-9._:-]{1,128}$").
- Request DTO: `StyleProfileTransitionRequest` (application/json, required).
- Responses: 200 `StyleProfileVersionView` (application/json).
- Errors: default `ApiProblem` (application/problem+json).
- Confidence: `confirmed-from-code`.
- Sources: `apps/api/src/main/java/dev/storyblock/api/http/StyleProfileController.java`, `apps/api/src/main/java/dev/storyblock/api/http/ApiSecurityConfiguration.java`, `apps/api/src/main/resources/openapi/storyblock-v1.yaml`, `apps/api/src/test/java/dev/storyblock/api/http/OpenApiContractTest.java`.


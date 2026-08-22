# V1 HTTP Contract

ADR-298 establishes the versioned HTTP boundary independently of later service
implementations. The machine-readable OpenAPI 3.1 document is packaged at
`apps/api/src/main/resources/openapi/storyblock-v1.yaml` and served without
authentication at `GET /v1/openapi.yaml`.

## Runtime Boundary

All documented routes are mapped and protected by their declared scope.
Canonical import, export submission, export-job status, artifact download,
deterministic render, adjacent metadata detection, commit, and access-key
lifecycle routes have concrete storage-backed handlers. Monitor packet,
submission, and stale-status routes are also storage-backed. Style profile,
immutable version, and lifecycle transition routes are storage-backed as well.
Durable style-analysis creation, status, paging, leasing, completion, and trace
download routes are storage-backed. For a route whose owning application service
is not implemented yet, a correctly authenticated
and well-formed request receives
`503 application/problem+json` with
`code=DEPENDENCY_UNAVAILABLE` and `Retry-After: 1`. There is no generated user,
form login, HTTP Basic fallback, or session state. Bearer credential validation
uses opaque, novel-bound credentials as described in
[`../security/scoped-access-and-audit.md`](../security/scoped-access-and-audit.md).

## Mutation Preconditions

Every `POST`, `PUT`, `PATCH`, and `DELETE` below `/v1/` requires both:

- `Idempotency-Key`: 1 to 200 characters.
- `If-Match`: one quoted strong canonical ETag in the form
  `"sha256:<64 lowercase hex digits>"`.

Missing preconditions return `428`. Invalid preconditions return `400`.
Wildcard `If-Match: *` is accepted only for collection creation at:

- `POST /v1/novels`
- `POST /v1/imports`
- `POST /v1/style-profiles`
- `POST /v1/internal/jobs/claims`

Other mutations must identify the exact current resource version. An owning
service maps a stale but valid ETag to `412` and returns the current revision ID
and hash, or the current style resource ETag for a style lifecycle conflict.
Request bodies are limited to 2 MiB for both fixed-length and chunked requests.
Style workers submit large canonical traces as a hash-verified gzip/base64
envelope so the expanded trace does not consume that request budget.

## Response Contracts

Revision-bearing responses use a strong `ETag`. Durable style analysis,
rewrite, and export submissions return `202` with a `Location` status URI.
Large style-analysis results use an opaque cursor. Errors use RFC problem JSON
with stable `code` and `request_id` fields.

`POST /v1/imports` returns `201` for a new atomic import and `200` when the same
idempotency key and canonical request are replayed. `POST
/v1/novels/{novelId}/exports` creates an immutable completed export and returns
its durable job URI. `GET /v1/jobs/{jobId}` exposes the result artifact URI, and
`GET /v1/artifacts/{artifactId}` returns offline-downloadable canonical bytes.
See [`../transfer/canonical-import-export.md`](../transfer/canonical-import-export.md).

`POST /v1/novels/{novelId}/renders` requires the requested immutable revision's
hash in `If-Match`, returns that hash as its response `ETag`, and emits the
canonical packet described in
[`../rendering/deterministic-renderer.md`](../rendering/deterministic-renderer.md).

`POST /v1/novels/{novelId}/detector-runs` additionally requires
`revision_hash` in the request body to equal `If-Match`. It returns stable,
revision-bound findings and the analyzed hash as `ETag`; see
[`../detection/adjacent-metadata-detector.md`](../detection/adjacent-metadata-detector.md).

`POST /v1/novels/{novelId}/monitor-packets` requires `novel:read` and returns
only the target, one or two neighbors on each side, resolved metadata, local
invariants, and in-window detector findings. `POST
/v1/novels/{novelId}/monitor-runs` requires `monitor:submit` and persists only
an evidence-bound finding or inert proposed operation. `GET
/v1/novels/{novelId}/monitor-runs/{monitorRunId}` requires `novel:read` and
derives stale state without rebasing. See
[`../monitoring/monitor-submission.md`](../monitoring/monitor-submission.md).

`POST /v1/style-profiles` creates an immutable novel-scoped profile. `POST
/v1/style-profiles/{profileId}/versions` creates an immutable `DRAFT` baseline;
it does not approve it. The transition resource enforces
`DRAFT -> CALIBRATING -> READY -> DEPRECATED`, records the authenticated actor,
and requires an explicit acknowledgement before generated or mixed corpus can
be promoted to `READY`. Profile and version GETs enforce the stored novel
boundary. See
[`../style/style-profiles-and-features.md`](../style/style-profiles-and-features.md).
The version payload carries a self-validating calibration profile when one is
available. Window, calibrated score, and anomaly decision schemas are defined
for the durable analysis API; their deterministic semantics are in
[`../style/rolling-windows-and-calibration.md`](../style/rolling-windows-and-calibration.md).

`POST /v1/novels/{novelId}/style-analyses` snapshots 1 to 1,000 immutable blocks
and returns the job and analysis URIs with a status ETag. Internal workers claim
one novel-scoped lease at a time and must submit the returned fencing ETag,
attempt, snapshot hash, profile hash, analyzer hash, and window hash. Expired
leases are reclaimable; duplicate canonical completion is idempotent. Summary
metadata and paged decisions remain in SQLite while detailed score traces are
gzip content-addressed artifacts with enforced expiry. See
[`../style/durable-analysis-jobs.md`](../style/durable-analysis-jobs.md).

The status policy is exactly `200`, `201`, `202`, `400`, `401`, `403`, `404`,
`409`, `410`, `412`, `413`, `422`, `428`, `429`, and `503`.

## Verification

Run the API contract and all prerequisite module tests offline after the
dependencies have been bootstrapped:

```bash
./mvnw -o -pl apps/api -am test
```

The tests parse every local OpenAPI reference, compare all 29 required routes,
exercise each remaining scaffold Spring mapping with its required scope, verify
all status-policy entries, and cover real bearer authentication, object-level
novel isolation, problem details, request IDs, ETags, idempotency, wildcard
restrictions, body limits, deterministic rendering, commits, complete detector
and monitor runs, the style profile promotion lifecycle, and an
import/export/job/artifact round trip. They also complete an exact 1,000-block
style job through create, claim, fenced result, paging, and gzip download.

# V1 HTTP Contract

ADR-298 establishes the versioned HTTP boundary independently of later service
implementations. The machine-readable OpenAPI 3.1 document is packaged at
`apps/api/src/main/resources/openapi/storyblock-v1.yaml` and served without
authentication at `GET /v1/openapi.yaml`.

## Runtime Boundary

All documented routes are mapped and protected by their declared scope.
Canonical import, export submission, export-job status, artifact download,
commit, and access-key lifecycle routes have concrete storage-backed handlers.
For a route whose owning application
service is not implemented yet, a correctly authenticated and well-formed
request receives `503 application/problem+json` with
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
and hash. Request bodies are limited to 2 MiB for both fixed-length and chunked
requests.

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

The status policy is exactly `200`, `201`, `202`, `400`, `401`, `403`, `404`,
`409`, `410`, `412`, `413`, `422`, `428`, `429`, and `503`.

## Verification

Run the API contract and all prerequisite module tests offline after the
dependencies have been bootstrapped:

```bash
./mvnw -o -pl apps/api -am test
```

The tests parse every local OpenAPI reference, compare all 22 required routes,
exercise each scaffold Spring mapping with its required scope, verify all
status-policy entries, and cover real bearer authentication, object-level novel
isolation, problem details, request IDs, ETags, idempotency, wildcard
restrictions, body limits, commits, and a complete import/export/job/artifact
round trip.

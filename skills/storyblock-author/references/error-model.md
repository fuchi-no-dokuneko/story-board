# StoryBlock error model

StoryBlock returns `application/problem+json` for API and filter failures. The stable base object is:

```json
{
  "type": "https://storyblock.example/problems/resource-not-found",
  "title": "Resource not found",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "detail": "The requested resource does not exist.",
  "instance": "/v1/novels/nov_...",
  "request_id": "req_..."
}
```

`type`, `title`, `status`, `code`, `detail`, `instance`, and `request_id` are always constructed by `ApiProblemFactory`. Domain-specific extension fields are inserted before `request_id`. Known extensions include current revision/ETag, stored and attempted hashes, candidate hash, validation `violations`/`warnings`, and policy-specific fields. Inspect the complete `problem` object in `--json` output; the CLI also lifts common fields and `violations` into `validation_issues`.

The response header `X-Request-Id` is always set. A caller may supply a 1–128 character value matching `[A-Za-z0-9._:-]+`; unsafe or absent values are replaced. Preserve it when correlating server logs.

## Status semantics

- `400`: malformed JSON, duplicate JSON members, invalid identifiers/parameters, invalid `If-Match`, invalid idempotency key, or bean validation failure. Correct locally; do not retry unchanged.
- `401`: credential missing or invalid/expired/revoked. Exit 4.
- `403`: authenticated principal lacks a scope/role, or cross-novel hiding is disabled and access is denied. Exit 4.
- `404`: missing resource; by default this also hides cross-novel access.
- `409`: idempotency/content/resource/lifecycle conflict. A reused idempotency key must retain its exact original content.
- `410`: expired style artifact.
- `412`: revision, analysis snapshot/lease, or style status precondition conflict. Refresh the relevant current resource before building a new operation.
- `413`: mutation body exceeds 2,097,152 bytes.
- `422`: deterministic validation rejected an edit; inspect `violations`, `warnings`, and `candidate_hash`.
- `428`: required `Idempotency-Key` or `If-Match` is missing.
- `429`: authenticated identity exceeded the configured limit (600 requests/minute by default). The filter emits `Retry-After: 60`.
- `500`: unexpected server failure.
- `503`: a storage or other required subsystem is temporarily unavailable.

All non-401/403 API responses exit 5, including retryable ones. Network and timeout failures exit 6.

## Mutation preconditions

Every `/v1` POST/PUT/PATCH/DELETE request is filtered before its controller:

- `Idempotency-Key` is required, must be nonblank, and is limited to 200 characters.
- `If-Match` is required.
- Collection creation routes accept only `*`: `/v1/novels`, `/v1/agent/novels`, `/v1/imports`, `/v1/style-profiles`, and `/v1/internal/jobs/claims`.
- Other mutations require exactly one quoted strong ETag: `"sha256:<64 lowercase hex>"`. Weak, unquoted, multiple, or wildcard values are rejected.

The body cap applies even when a controller does not use a JSON DTO.

## Strict JSON

The local parser and server both reject duplicate object keys. The local schema validator reports JSON-pointer-like paths. It implements the schema features used by this package plus StoryBlock checks for canonical timestamps, Han counts, normalized nonblank registration values, and range endpoint guards.

## Retry guidance

Safe reads may be retried after transient network/5xx/429 failures. Honor `Retry-After` when present.

For a mutation, retain the exact canonical JSON value, `Idempotency-Key`, resource IDs, candidate timestamp, and precondition. If the response was lost, retry those same values. Never “repair” an uncertain request by reusing its key with a changed body. A confirmed 412 or semantic 422 requires a newly constructed operation, not a blind retry.

If the server returns non-JSON error content, the CLI synthesizes `UNPARSEABLE_ERROR_RESPONSE`, preserves the HTTP status/body text, and still exits 4 or 5 as appropriate.

# Canonical Import And Export

ADR-299 defines two versioned transfer formats:

- `canonical-revision` contains one canonical genesis revision.
- `canonical-package` contains a complete linear revision chain, the operation
  producing each non-genesis revision, and portable artifacts.

Both use deterministic JSON serialization and lowercase SHA-256 identifiers.
Package version `1.0.0` and canonical revision schema `1.0.0` are the only
accepted versions. Unknown fields, duplicate JSON keys, malformed IDs, broken
lineage, count drift, hash drift, non-canonical timestamps or Base64, and
operation replay drift are rejected.

## HTTP Flow

Import a new novel with `POST /v1/imports`, `If-Match: *`, an
`Idempotency-Key`, and this envelope:

```json
{
  "format": "canonical-revision",
  "document": {"schema_version": "1.0.0", "content_hash": "sha256:..."}
}
```

Use `canonical-package` and a package document for complete history. A first
import returns `201`, `Location`, and the head `ETag`. An identical idempotent
retry returns the original result with `200`; reusing the key for different
content returns `409`.

Create an export with `POST /v1/novels/{novelId}/exports`, the exact current
head `If-Match`, an `Idempotency-Key`, and:

```json
{
  "revision_id": "rev_...",
  "format": "canonical-package"
}
```

The `202` response points to `/v1/jobs/{jobId}`. A succeeded job contains a
`result_uri`; downloading it returns immutable canonical bytes with an `ETag`,
`Content-Disposition: attachment`, and `X-Artifact-Codec`.

## Integrity And Recovery

The SQLite import writes the novel, revisions, operations, portable artifacts,
tombstones, head checkpoint, projection, and idempotency receipt in one
transaction. Any validation, constraint, or injected stage failure rolls back
the entire import. Export artifact and completed job creation are likewise one
transaction.

Import followed by package export preserves the complete canonical chain and
head hash. Generated export artifacts are deliberately not embedded in later
packages, preventing recursive package growth; imported portable artifacts are
preserved. The API database path is configured with
`storyblock.database.path` and defaults to `data/storyblock.db`.

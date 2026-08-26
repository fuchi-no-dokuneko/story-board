# Durable Style Analysis Jobs

ADR-306 implements the asynchronous boundary for versioned style analysis. A
request captures one immutable revision/profile snapshot, a worker evaluates
that snapshot under a fenced lease, and SQLite commits one canonical result and
one content-addressed trace artifact atomically.

## Snapshot And Request

`POST /v1/novels/{novelId}/style-analyses` requires `style:analyze`, the exact
revision hash in `If-Match`, and these profile identities in the request body:

- `revision_id`, `profile_id`, and `profile_version_id`;
- optional inclusive `from_block_id` and `to_block_id`;
- the masking lexicon used by the profile feature contract;
- bounded attempt and retention settings.

The selected range must contain 1 to 1,000 blocks. The stored snapshot includes
the complete normalized blocks, immutable profile version, masking vocabulary,
revision hash, profile-version hash, analyzer-contract hash, and window-config
hash. The snapshot hash covers all of them. Later edits or profile lifecycle
changes therefore cannot alter an accepted job.

Creation is idempotent per novel. Reusing a key with the same canonical request
returns the original job; reusing it with different snapshot or policy data
returns `409`.

## Lease State Machine

```text
QUEUED -> RUNNING -> SUCCEEDED
                  -> FAILED
```

`POST /v1/internal/jobs/claims` requires `worker:execute` and claims the oldest
eligible job for one novel. The returned strong ETag is the fencing hash for
the exact owner, attempt, expiry, snapshot, and state. A lease owns the
half-open interval from claim time up to, but not including, `lease_until`.

If a worker disappears, a later claim can reclaim its expired job and increments
the attempt. The old owner and attempt can no longer complete it. When the last
allowed lease expires, the next claim sweep marks the job `failed` with
`attempts_exhausted`. Claim receipts, including no-work responses, are
idempotent so a retried network request cannot claim a second job accidentally.

## Completion And Artifacts

`POST /v1/internal/jobs/{jobId}/results` requires the claim ETag and repeats all
four immutable version hashes. The owner, attempt, ETag, versions, completion
time, summary totals, window ordinals, and trace hash are validated before one
transaction writes the result.

Operational window rows contain compact decisions for cursor paging. Detailed
feature vectors and scores are kept only in canonical JSON trace bytes. Workers
gzip those bytes and submit a base64 envelope containing the compressed bytes,
stored-byte SHA-256 hash, and verified expanded size. The artifact ID is derived
from the analysis ID and content hash. The server rejects invalid gzip,
noncanonical JSON, hash mismatches, and expanded content above 16 MiB.

`GET /v1/style-analyses/{analysisId}` returns state and result metadata. `GET
/v1/style-analyses/{analysisId}/windows` pages compact findings with an opaque,
analysis-bound cursor. `GET /v1/artifacts/{artifactId}` returns the gzip bytes
with `X-Artifact-Codec: gzip`; after `retention_until`, it returns `410`.
Result, receipt, window, and artifact metadata rows are immutable. Repeating the
same canonical result returns the original result; a different result for the
same completed job returns `409`.

## Running The Worker

Build the executable worker JAR offline after dependencies are installed:

```bash
./mvnw -o -pl apps/style-worker -am package
```

Configure it with Spring properties or their environment equivalents. The
bearer token needs `worker:execute` for exactly one novel and is never logged.

```text
STORYBLOCK_WORKER_ENABLED=true
STORYBLOCK_WORKER_API_BASE_URL=https://127.0.0.1:8443/
STORYBLOCK_WORKER_TOKEN=<opaque novel-bound token>
STORYBLOCK_WORKER_NOVEL_ID=nov_<uuid>
STORYBLOCK_WORKER_ID=style-worker-1
STORYBLOCK_WORKER_LEASE_SECONDS=300
STORYBLOCK_WORKER_POLL_INTERVAL=PT5S
STORYBLOCK_WORKER_RUN_ONCE=false
```

The runtime forces the IPv4 stack, rejects IPv6 API origins and redirects, and
does not include the credential in settings output or protocol errors. The
container profile mounts the public trust store generated for the API's
self-signed leaf; it never mounts the API private key.

## Verification

```bash
./mvnw -o -pl modules/storage-sqlite,apps/api,apps/style-worker -am test
```

Tests cover expired-lease reclamation, stale-owner rejection, exact-expiry
rejection, duplicate completion, immutable result storage, trace hash and gzip
validation, worker credential redaction, cursor paging, and completion of an
exact 1,000-block analysis through the HTTP API.

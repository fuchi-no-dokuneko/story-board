# Scoped Access And Redacted Audit

StoryBlock authenticates machine requests with opaque bearer credentials bound
to one novel, one actor, an explicit scope set, and an expiry. Credential
secrets are returned only by the successful issuance response. SQLite stores a
32-byte HMAC-SHA-256 digest, never the bearer token or its secret component.

## Runtime Configuration

Set `storyblock.security.pepper` (environment variable
`STORYBLOCK_SECURITY_PEPPER`) to independently generated secret material of at
least 32 bytes. The service refuses to start without it. The pepper must be
mounted separately from the SQLite database and rotated through an explicit key
replacement procedure because changing it invalidates every issued credential.

An optional `storyblock.security.owner-token` (environment variable
`STORYBLOCK_SECURITY_OWNER_TOKEN`) of at least 32 characters enables bootstrap
administration. It is compared by SHA-256 digest in constant time and is never
persisted. Omit it after issuing normal administration credentials when
bootstrap access is no longer required.

`storyblock.security.hide-cross-novel` defaults to `true`, returning the same
404 contract for missing and out-of-bound objects. Setting it to `false` returns
a typed 403 for trusted deployments where existence disclosure is acceptable.

## Authorization Rules

- Every stored key belongs to exactly one novel.
- Route scopes and object ownership are both enforced.
- Non-owner credentials may delegate only scopes they already hold, and the
  delegated key cannot outlive its issuer.
- Job, artifact, and key identifiers are resolved to their owning novel before
  controller access.
- URL and body novel identifiers must agree with each other and with the key.
- Expired and revoked credentials return the same generic 401 response as an
  invalid secret.
- `last_used_at` writes are throttled to one update per five-minute interval.

## Audit Boundary

Audit rows contain only typed identifiers, action/result enums, timestamps, and
canonical SHA-256 hashes. They have no columns for request bodies, prose,
prompts, corpora, model output, bearer tokens, or credential digests. Commit
rows and their audit row are written in one SQLite transaction, including the
head compare-and-swap.

Access keys cannot be deleted and audit rows cannot be modified. Audit deletion
is intentionally permitted so an operator can apply a retention period that is
independent of immutable narrative history. Retention jobs must delete only
`audit_events`; they must never mutate access keys, operations, or revisions.

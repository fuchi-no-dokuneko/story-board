# ADR 0007: Stateless Preview Commit Tokens

- Status: Accepted
- Date: 2026-08-21
- Owner: StoryBlock local implementation

## Decision

A preview is not persisted. It returns the normalized operation, base revision
and head hashes, candidate hash, expiry, actor/key identity, and an opaque HMAC
token over a versioned canonical payload containing those fields, the novel ID,
the normalized-operation hash, and a random nonce. The token carries a signing
key ID so keys can rotate, but never contains the signing key. Commit resubmits
the operation and token. The server authenticates the actor, checks the novel
and key binding, verifies expiry and signature in constant time, reloads the
immutable base, recomputes the candidate, and performs the normal head
compare-and-swap transaction.

An idempotency key is independent of the preview token. Reusing a key with the
same operation hash returns the original result; reuse with another hash is a
conflict.

## Consequences

A stolen token cannot be applied by another key, novel, operation, or head and
cannot bypass validation. A candidate hash alone is never commit authority.
Replay during the validity window is resolved only through the independent
idempotency record; an expired or retired-key token must be previewed again.

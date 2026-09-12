# ADR 0002: Canonical JSON and Hashing

- Status: Accepted
- Date: 2026-08-21
- Owner: StoryBlock local implementation

## Decision

Canonical interchange uses UTF-8 JSON with lexicographically ordered object
keys, deterministic scalar formatting, strict unknown-field handling within a
major schema version, and SHA-256 identifiers prefixed with `sha256:`. Content
hash input is the canonical content projection: it excludes the envelope's own
`content_hash` and all derived render, cache, detector, monitor, and analysis
data. The final envelope is serialized only after that hash is computed.

Schema, normalizer, renderer, validator, and analyzer versions are explicit.
Canonical golden bytes and hashes are treated as compatibility fixtures.

## Consequences

JSON maps must not rely on insertion order. Floating-point values that affect a
canonical hash require an explicit normalized representation or fixed decimal
contract. A new field cannot enter the hash projection without a schema-version
and golden-fixture change.

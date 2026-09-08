# ADR 0006: Stable Identities and Ordering

- Status: Accepted
- Date: 2026-08-21
- Owner: StoryBlock local implementation

## Decision

Novel, chapter, scene, block, block-version, revision, operation, proposal, job,
artifact, and key identities use prefixed UUIDv7 values. References never use
array positions. Ordering uses fixed-width unsigned fractional keys and inserts
at the midpoint between adjacent keys; a dedicated deterministic rebalance is
required when no midpoint remains.

## Consequences

Movement changes only a block's order key and revision selection. Rewriting
retains the block ID when semantically one-to-one but always creates a new
block-version ID.

# Atomic commit, replay, checkpoint, and restore

ADR-297 implements the immutable SQLite write path described in specification
sections 6.1-6.5 and 12.2-12.5. Canonical revisions and typed operations are
append-only. `head_block_projection` is replaceable derived state, while the
`novels` row owns the current compare-and-swap head.

## Commit boundary

`CommitService` normalizes and validates a candidate outside a transaction. The
SQLite adapter then holds one short `BEGIN IMMEDIATE` transaction and performs:

1. idempotency-key lookup;
2. expected head identity and hash comparison;
3. operation and revision append;
4. block tombstone append;
5. head projection rebuild and optional checkpoint append;
6. guarded head update and commit.

Any exception rolls back the complete sequence. A stale head raises
`StaleHeadException` with HTTP mapping 412. Reusing a key with the identical
normalized operation hash returns the original result; a different hash raises
`IdempotencyConflictException` with HTTP mapping 409. Deterministic validation
failures use 422 and never enter the write transaction.

Database triggers reject updates or deletes against revisions, operations,
checkpoints, and tombstones. Restore uses `restore_revision_content`, selecting
historical content into a newly identified child revision. It never rewinds the
head or removes the deletion and its tombstone.

## Checkpoints and replay

Sequence zero receives a genesis checkpoint. Later checkpoints use deterministic
`gzip-v1` compression and are created after 100 revisions by default, or when
operation payloads since the previous checkpoint reach 1 MiB. These thresholds
are configurable through `CheckpointPolicy`.

`ReplayService.materialize` verifies the nearest checkpoint against its canonical
envelope, relational revision, identity, and hash, then applies every subsequent
typed operation in sequence. Each intermediate result must match both the stored
operation result hash and revision record. `materializeFull` starts at sequence
zero and reconstructs historical restore targets from already replayed state, so
it does not trust a later checkpoint.

Build the executable verifier and check every novel head with:

```bash
./mvnw -o -pl apps/cli -am package
java -jar apps/cli/target/storyblock-cli-0.1.0-SNAPSHOT.jar \
  replay-verify /absolute/path/to/storyblock.db
```

Exit status is 0 only when all full replays reproduce their stored head reference
and hash. Status 1 means verification or storage failure; status 2 means invalid
command syntax.

## Acceptance coverage

`SqliteRevisionStoreTest` covers stale-head no-write behavior, exact and conflicting
idempotency retries, pre-transaction validation rejection, rollback after every
injected transaction stage, interval checkpoints plus trailing replay, full replay
of every novel, compressed checkpoint equality, tombstone restore, and append-only
triggers. `EditOperationCanonicalMapperTest` round-trips all ten operation types.

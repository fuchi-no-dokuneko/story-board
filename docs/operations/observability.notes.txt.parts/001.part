# Observability and safe logging

The API exposes aggregate health at `/actuator/health`. Component health and
the metric index require the bootstrap owner's operator role. Per-novel
credentials can never receive that role or read cross-novel operations data.

The metric index includes the specification's SQLite latency and contention,
WAL/checkpoint, worker queue, analysis/rewrite duration, stale proposal,
detector finding, artifact storage, backup age, and authorization denial names.
Detector and authorization counters use bounded `code` and `reason` tags.

Health contributors cover SQLite read/write access, Flyway migration version,
WAL checkpoint state, backup freshness, worker queue age, and artifact storage.
Configure the production thresholds and backup manifest directory with:

```text
STORYBLOCK_HEALTH_MAX_WAL_BYTES
STORYBLOCK_HEALTH_MAX_JOB_AGE
STORYBLOCK_BACKUP_MANIFEST_DIRECTORY
STORYBLOCK_BACKUP_MAX_AGE
```

Console logs use ECS JSON. Application code logs identifiers and state only;
bearer credentials, canonical text, request bodies, prompts, corpus content,
and raw model responses are excluded. Security and immutable audit records stay
in separate channels: operational logs are external, while redacted audit rows
remain in SQLite under their own retention policy.

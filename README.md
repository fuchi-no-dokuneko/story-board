# StoryBlock Engine

StoryBlock Engine is a local-first service for immutable, evidence-bound editing
of long-form narrative blocks. The canonical write path uses typed operations,
deterministic validation, preview tokens, and compare-and-swap commits.

The first release provides the API, SQLite storage, style and rewrite workers,
and a lightweight operator console.

## Requirements

- Java 21
- Maven 3.6.3 or newer, or the included Maven Wrapper
- SQLite 3 for operator diagnostics

## Build

```bash
./install.sh
./mvnw verify
```

## Run

Set two local values of at least 32 characters, then start the API:

```bash
export STORYBLOCK_SECURITY_OWNER_TOKEN='replace-with-a-local-owner-token-32+'
export STORYBLOCK_SECURITY_PEPPER='replace-with-a-local-server-pepper-32+'
./mvnw -pl apps/api -am package -DskipTests
java -Djava.net.preferIPv4Stack=true \
  -jar apps/api/target/storyblock-api-0.1.0-SNAPSHOT.jar
```

Open `http://127.0.0.1:8080/` for the API console. Container users can place
the same values in an untracked `.env` and run `docker compose up --build`.

The bootstrap is unprivileged and keeps downloaded tooling in the current
user's Maven cache. The Unix wrapper download and every Maven JVM are configured
for IPv4 only.

Verify every SQLite novel head from the full immutable operation log with:

```bash
java -jar apps/cli/target/storyblock-cli-0.1.0-SNAPSHOT.jar \
  replay-verify data/storyblock.db
```

The command emits one canonical JSON report and exits nonzero when any head
cannot be reproduced. It requires an existing database file, preventing a path
typo from being reported as an empty valid store.

## HTTP Contract

The versioned REST contract is served at `GET /v1/openapi.yaml` and documented
in [`docs/api/v1-http-contract.md`](docs/api/v1-http-contract.md). Contract
routes fail closed: authentication and mutation preconditions are active now.
Canonical import, export jobs, job status, and artifact download are implemented;
deterministic revision rendering, scoped bearer-key issuance, revocation, atomic
commits, adjacent metadata detection, and redacted audit events are also
implemented. Bounded monitor packets, immutable finding/proposal submission,
and stale-output invalidation are documented in
[`docs/monitoring/monitor-submission.md`](docs/monitoring/monitor-submission.md).
Immutable style profiles, explicit baseline promotion, and versioned feature
channels are implemented and documented in
[`docs/style/style-profiles-and-features.md`](docs/style/style-profiles-and-features.md).
Rolling-window stratification, reproducible calibration, and anomaly decisions
are documented in
[`docs/style/rolling-windows-and-calibration.md`](docs/style/rolling-windows-and-calibration.md).
Durable style-analysis snapshots, leases, result paging, compressed artifacts,
and the executable worker are documented in
[`docs/style/durable-analysis-jobs.md`](docs/style/durable-analysis-jobs.md).
The bounded, proposal-only LLM process and its strict model gateway protocol are
documented in
[`docs/rewrite/sandboxed-llm-worker.md`](docs/rewrite/sandboxed-llm-worker.md).
Services assigned to later ADRs return a typed `503` until
installed. The
transfer format and recovery guarantees are documented in
[`docs/transfer/canonical-import-export.md`](docs/transfer/canonical-import-export.md).
Security configuration and audit retention are documented in
[`docs/security/scoped-access-and-audit.md`](docs/security/scoped-access-and-audit.md).
Deterministic text, offset, metadata-state, and scene-boundary semantics are
documented in
[`docs/rendering/deterministic-renderer.md`](docs/rendering/deterministic-renderer.md).
Adjacent-state rules and reset-boundary behavior are documented in
[`docs/detection/adjacent-metadata-detector.md`](docs/detection/adjacent-metadata-detector.md).

## Modules

- `apps/api`: Spring MVC API and commit coordinator
- `apps/style-worker`: durable style-analysis worker
- `apps/llm-worker`: isolated one-shot, proposal-only rewrite worker
- `apps/cli`: replay, backup, restore, and operator commands
- `modules/rewrite`: immutable rewrite input and text-proposal contracts
- `modules/*`: other framework-independent domain services and adapters
- `modules/storage-sqlite/src/main/resources/db/migration`: Flyway migrations
- `tests/architecture-tests`: executable module-boundary checks

The authoritative product requirements are documented in `docs/specification.md`
and the architectural decisions in `docs/adr`.

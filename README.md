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
```

`install.sh` requires Java 21 and writes the Maven distribution, dependency
cache, generated secrets, self-signed certificate, installed server, database,
logs, and temporary files only below `.local/storyblock/` in this repository.
It does not use `sudo`, require root, write to system directories, or upload a
certificate.

## Run

```bash
./scripts/local-server.sh start
./scripts/local-server.sh status
```

Open `https://127.0.0.1:8443/` for the API console. The server uses an
automatically generated self-signed leaf, so a browser trust warning is
expected. Stop it with `./scripts/local-server.sh stop`. Container users can
run `docker compose up --build api`; the API generates its own self-signed leaf
inside a private local volume and exposes no reverse proxy.

The Library and API console share the **Operator access** control at the top of
the page. Paste the token from `.local/storyblock/secrets/owner-token` and choose
**Use token**. The page keeps it only in JavaScript memory for the current page,
never stores it in browser storage, and sends it only to the same StoryBlock
origin. Choose **Clear** to remove it and purge protected library content.

### Trusted-LAN novel library

For local-first admin reading and AI manuscript registration, start the
dedicated trusted profile:

```bash
./scripts/run-trusted-lan.sh
```

Open `https://127.0.0.1:8443/`. The first run creates a self-signed leaf
certificate, keystore password, and local security pepper under the ignored
`.local/trusted-lan/` directory. There is no CA and no external certificate or
key-distribution service; the private certificate key remains on this host.
The HTTPS protocol still performs its normal per-connection session-key
agreement.

This profile deliberately treats every request as the owner. It accepts only
an explicit loopback, RFC1918, or Tailscale/CGNAT IPv4 bind and must never be
publicly routed. Set `STORYBLOCK_LAN_BIND_ADDRESS`, `STORYBLOCK_LAN_PORT`, and
`STORYBLOCK_DATABASE_PATH` when a different private interface or database is
needed. IPv6 is not supported.

The browser library can list, search, and read current persisted revisions but
contains no write or delete controls. AI authors register manuscripts through
the tracked `skills/storyblock-author` skill and `POST /v1/agent/novels`; its
`verify` command compares the source Han sequence and SHA-256 digest with the
revision read back from storage.

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

Encrypted live backup and isolated restore drills are documented in
[`docs/operations/backup-and-restore.md`](docs/operations/backup-and-restore.md).

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

## GUI acceptance and narrated demos

The novel library has executable daily Gherkin UAT plus timed English and
Traditional Chinese Cantonese recording guides. Setup, fixture boundaries,
reports, Sonar import, and the feature coverage matrix are documented in
[`acceptance/README.md`](acceptance/README.md).

# StoryBlock Engine

StoryBlock Engine is a local-first service for immutable, evidence-bound editing
of long-form narrative blocks. The canonical write path uses typed operations,
deterministic validation, preview tokens, and compare-and-swap commits.

This repository is intentionally local-only. It has no configured Git remote and
must not be deployed or pushed without an explicit owner decision.

## Requirements

- Java 21
- Maven 3.6.3 or newer, or the included Maven Wrapper
- SQLite 3 for operator diagnostics

## Build

```bash
./install.sh
./mvnw verify
```

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
commits, and redacted audit events are also implemented. Services assigned to
later ADRs return a typed `503` until installed. The
transfer format and recovery guarantees are documented in
[`docs/transfer/canonical-import-export.md`](docs/transfer/canonical-import-export.md).
Security configuration and audit retention are documented in
[`docs/security/scoped-access-and-audit.md`](docs/security/scoped-access-and-audit.md).
Deterministic text, offset, metadata-state, and scene-boundary semantics are
documented in
[`docs/rendering/deterministic-renderer.md`](docs/rendering/deterministic-renderer.md).

## Modules

- `apps/api`: Spring MVC API and commit coordinator
- `apps/style-worker`: durable style-analysis worker
- `apps/llm-worker`: proposal-only rewrite worker
- `apps/cli`: replay, backup, restore, and operator commands
- `modules/*`: framework-independent domain services and adapters
- `modules/storage-sqlite/src/main/resources/db/migration`: Flyway migrations
- `tests/architecture-tests`: executable module-boundary checks

The authoritative product requirements are documented in `docs/specification.md`
and the architectural decisions in `docs/adr`.

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

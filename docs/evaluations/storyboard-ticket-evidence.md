# StoryBoard ticket evidence

Local-only completion index for branch `tickets/storyblock-260823`. The full
reactor run on 2026-08-23 completed 160 tests with zero failures and zero
errors. StoryBoard is intentionally not pushed to a cloud remote.

## Parent epics

| Epic | Child tickets | Result |
|---|---|---|
| ADR-44 | ADR-291 to ADR-294 | Architecture, contracts, modular build, and WAL spike complete |
| ADR-45 | ADR-295 to ADR-297 | Immutable editing, preview, commit, replay, and restore complete |
| ADR-46 | ADR-298 to ADR-300 | HTTP contracts, transfer, authorization, and audit complete |
| ADR-47 | ADR-301 to ADR-303 | Renderer, detector, and monitor protocol complete |
| ADR-48 | ADR-304 to ADR-306 | Style profiles, calibration, policy, and durable jobs complete |
| ADR-49 | ADR-307 to ADR-309 | Sandboxed rewrite, gates, and manual review complete |
| ADR-50 | ADR-310 to ADR-314 | Migration, deployment, observability, backup, and restore complete |
| ADR-51 | ADR-315 to ADR-320 | Tests, evaluations, SLOs, decisions, and scale-out gate complete |

## Delivery commits

| Ticket | Commit | Primary evidence |
|---|---|---|
| ADR-291 | `d33fd32` | `docs/adr/README.md` |
| ADR-292 | `132866b` | root `pom.xml` and module build |
| ADR-293 | `a870156` | domain and canonical contract suites |
| ADR-294 | `515db87` | `docs/spikes/ADR-294-sqlite-wal.md` |
| ADR-295 | `3735271` | immutable editing core suite |
| ADR-296 | `33d52a6` | preview, diff, and validator suites |
| ADR-297 | `ea33919` | atomic commit, replay, checkpoint, and restore suites |
| ADR-298 | `da4aba6` | OpenAPI and HTTP contract suites |
| ADR-299 | `4be7184` | canonical transfer suites |
| ADR-300 | `e2c07ff` | scoped security and audit suites |
| ADR-301 | `2494b3d` | deterministic renderer golden suite |
| ADR-302 | `6b2d400` | adjacent metadata detector golden suite |
| ADR-303 | `dd317e5` | monitor submission and invalidation suite |
| ADR-304 | `d76166c` | style profile and feature suites |
| ADR-305 | `7178c63` | calibration and anomaly-policy suites |
| ADR-306 | `0cbdcc3` | durable analysis API and storage suites |
| ADR-307 | `e8ce77e` | sandboxed LLM worker suite |
| ADR-308 | `cc109f1` | rewrite policy and operator review contracts |
| ADR-309 | `81ffc33` | stale proposal and manual commit workflow |
| ADR-310 | `210d73a` | Flyway V001-V008 startup verification |
| ADR-311 | `3df4a93` | secure deployment topology test and runbook |
| ADR-312 | `f68a3b0` | metrics, health, authorization, and logging tests |
| ADR-313 | `f23fe8d` | encrypted live backup and retention scripts |
| ADR-314 | `c95aab9` | isolated restore/replay/render-hash drill |
| ADR-315 | `210815c` | unit, property, and golden suites; `text-coverage.txt` |
| ADR-316 | `a0516ef` | crash, concurrency, rate-limit, and security suites |
| ADR-317 | `031e54a` | `docs/evaluations/ADR-317.md` |
| ADR-318 | `37b423f` | `docs/evaluations/ADR-318.md` |
| ADR-319 | `9500677` | product-policy verifier and ADR 0008 |
| ADR-320 | `2fb19f9` | `docs/evaluations/ADR-320.md` and trigger gate |

## Reproduction

```bash
timeout 60s ./mvnw -q test
scripts/run-adr317-evaluation.sh
scripts/run-slo-qualification.sh
scripts/verify-product-policy.sh
scripts/verify-postgresql-gate.sh
```

The ADR-317 and ADR-318 scripts retain detailed machine-readable reports under
ignored `artifacts/`. Threshold failures are written to those reports and
produce nonzero command exits.

# ADR 0008: Initial product and operating policy

- Status: Accepted
- Owner: fuchi-no-dokuneko
- Decision date: 2026-08-22
- Policy version: product-policy-1.0.0

| ID | Owner | Date | Decision | Rationale | Affected version |
|---|---|---|---|---|---|
| O-01 | fuchi-no-dokuneko | 2026-08-22 | The product name is StoryBlock Engine. | Keeps API and package naming stable for v1. | product-policy-1.0.0 |
| O-02 | fuchi-no-dokuneko | 2026-08-22 | Every LLM rewrite requires explicit human approval before commit. | A generated proposal cannot silently become canon. | rewrite-policy-1.0.0 |
| O-03 | fuchi-no-dokuneko | 2026-08-22 | Chinese quotes and ellipses use the versioned deterministic boundary parser; ambiguous splits require an explicit anchor. | Keeps replay deterministic. | sentence-boundary-1.0.0 |
| O-04 | fuchi-no-dokuneko | 2026-08-22 | The first style worker is Java. | It shares exact contracts with the API and can be replaced behind the worker protocol. | rewrite-model-1.0.0 |
| O-05 | fuchi-no-dokuneko | 2026-08-22 | Target corpora must be owner-authored, licensed, or public domain with source hash and provenance. | Enables enforceable near-copy policy. | style-features-1.0.0; long-ngram-1.0.0 |
| O-06 | fuchi-no-dokuneko | 2026-08-22 | Initial RPO is one hour and RTO is four hours. | Appropriate for the first private deployment and measurable by restore drills. | backup-policy-1.0.0 |
| O-07 | fuchi-no-dokuneko | 2026-08-22 | v1 relies on encrypted disks and separately keyed encrypted backups. | Per-novel field encryption is deferred until required. | backup-policy-1.0.0 |
| O-08 | fuchi-no-dokuneko | 2026-08-22 | v1 uses immutable revisions and head CAS; branches and merges are v2 work. | Avoids premature merge semantics. | canonical-revision-1.0.0 |
| O-09 | fuchi-no-dokuneko | 2026-08-22 | Explicit scene resets are informational; unsupported continuous-boundary changes remain warnings or errors. | Preserves intentional transitions. | detector-1.0.0 |
| O-10 | fuchi-no-dokuneko | 2026-08-22 | Full analysis artifacts expire after 30 days; compact summaries remain durable. | Bounds storage without losing decisions. | style-analysis-1.0.0 |

## Version impact

This ADR introduces `product-policy-1.0.0` and `backup-policy-1.0.0`. It
ratifies the listed v1 wire and analysis contracts without changing their
fields or semantics, so those contract versions remain at 1.0.0. Any change to
an O-01 through O-10 decision requires a successor ADR and an increment to the
affected version listed above.

Changes to these decisions require a new ADR and the relevant contract or
policy version increment.

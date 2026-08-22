# ADR 0008: Initial product and operating policy

- Status: Accepted
- Owner: fuchi-no-dokuneko
- Decision date: 2026-08-22

| ID | Decision | Rationale |
|---|---|---|
| O-01 | The product name is StoryBlock Engine. | Keeps API and package naming stable for v1. |
| O-02 | Every LLM rewrite requires explicit human approval before commit. | A generated proposal cannot silently become canon. |
| O-03 | Chinese quotes and ellipses use the versioned deterministic boundary parser; ambiguous splits require an explicit anchor. | Keeps replay deterministic. |
| O-04 | The first style worker is Java. | It shares exact contracts with the API and can be replaced behind the worker protocol. |
| O-05 | Target corpora must be owner-authored, licensed, or public domain with source hash and provenance. | Enables enforceable near-copy policy. |
| O-06 | Initial RPO is one hour and RTO is four hours. | Appropriate for the first private deployment and measurable by restore drills. |
| O-07 | v1 relies on encrypted disks and separately keyed encrypted backups. | Per-novel field encryption is deferred until required. |
| O-08 | v1 uses immutable revisions and head CAS; branches and merges are v2 work. | Avoids premature merge semantics. |
| O-09 | Explicit scene resets are informational; unsupported continuous-boundary changes remain warnings or errors. | Preserves intentional transitions. |
| O-10 | Full analysis artifacts expire after 30 days; compact summaries remain durable. | Bounds storage without losing decisions. |

Changes to these decisions require a new ADR and the relevant contract or
policy version increment.

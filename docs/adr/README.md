# Architecture Decision Records

ADRs are immutable after acceptance. A superseding decision receives a new
record and links the replaced decision.

The Phase 0 set was approved on 2026-08-21 for Jira work item `ADR-291`.

| ADR | Concern | Status |
|---|---|---|
| 0001 | Narrative block semantics | Accepted |
| 0002 | Canonical JSON and hashing | Accepted |
| 0003 | SQLite JDBC persistence | Accepted |
| 0004 | Scoped opaque authorization | Accepted |
| 0005 | Versioned sentence and grapheme parsing | Accepted |
| 0006 | Stable identities and fractional ordering | Accepted |
| 0007 | Stateless preview commit tokens | Accepted |
| 0008 | Initial product and operating policy | Accepted |
| 0009 | Application-terminated self-signed TLS | Accepted |

Together these records freeze every Phase 0 canonical-contract decision. ADR
0007 resolves the security and replay behavior for committing a preview that
the server does not persist.

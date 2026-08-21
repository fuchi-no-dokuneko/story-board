# ADR 0004: Scoped Opaque Authorization

- Status: Accepted
- Date: 2026-08-21
- Owner: StoryBlock local implementation

## Decision

Machine access uses one-time opaque secrets bound server-side to exactly one
novel, actor, expiry, and scope set. Only an HMAC-SHA-256 digest made with a
separately mounted server pepper is persisted. Authorization derives novel
scope from the authenticated key and rejects conflicting URL or body identities.

Workers receive minimum-purpose credentials. Rewrite and monitor workers never
receive commit credentials or direct database access.

## Consequences

Secrets are transport-injected and redacted from logs, errors, metrics, prompts,
and audit payloads. Human multi-user deployment requires a later OIDC/session
identity layer rather than sharing machine keys.

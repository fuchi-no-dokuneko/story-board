# Monitor Submission and Invalidation

ADR-303 defines a monitor as a read-only, local observer. It receives a bounded
packet and can submit data, but it never receives database, global-wiki, secret,
edit, or commit capabilities.

## Packet Boundary

`POST /v1/novels/{novelId}/monitor-packets` accepts one target block and a
neighbor count of one or two. The response contains at most five blocks: the
target and the available neighbors on each side. It includes the deterministic
render, resolved metadata, in-window detector findings, block fingerprints, and
local narrative invariants. Its only declared tools are `submit_finding` and
`submit_proposed_operation`.

The packet endpoint requires `novel:read`. The submission endpoint separately
requires `monitor:submit`, so a worker credential needs both scopes to fetch and
submit while still lacking `novel:propose` and `novel:commit`.

## Evidence-Bound Outputs

A submission is exactly one of:

- `finding`: code, severity, message, and text evidence.
- `proposed_operation`: one typed operation plus text evidence.

Every affected block must be inside the original packet window and have an
exact Unicode-grapheme evidence span whose quote and quote hash match the source
revision text. Proposed operations must identify that revision and may refer
only to blocks in the packet. Global revision restore proposals are rejected.
No submitted proposal is previewed, applied, committed, or promoted to canon.

SQLite persists an immutable `monitor_runs` row and exactly one child row in
`monitor_issues` or `monitor_proposed_operations`. A repeated idempotency key
with the same canonical request returns the original IDs; another payload is a
conflict. Request and actor identifiers are retained for traceability, but
credential secrets are never accepted or stored in monitor payloads.

## Stale State

Status is derived at read time. A run is `stale` when any of these conditions
holds:

- the novel head revision or hash changed;
- an affected block disappeared;
- an affected block version, text, metadata, or extensions changed;
- the monitor rule version changed.

The original revision hash, affected fingerprints, output, and evidence remain
unchanged. Every response states `rebase_allowed: false`; clients must request a
new packet and submit a new run instead of rebasing an old finding automatically.

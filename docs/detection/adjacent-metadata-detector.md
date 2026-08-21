# Adjacent Metadata Detection

The adjacent metadata detector is a deterministic, read-only analysis over one
immutable revision and an optional inclusive block range. It resolves metadata
with the canonical renderer, inspects each selected block with its previous and
next block as context, and compares adjacent scene boundaries. A detector run
never edits canonical text, metadata, scene seeds, or revision history.

## Findings

Every finding contains a revision-derived stable `finding_id`, code, default
severity, revision ID and hash, detector rule version, affected block and scene
IDs, up to three context block IDs, and structured evidence. Repeating a run
with the same revision hash, rule version, and range produces byte-identical
canonical JSON and finding IDs.

The detector emits these rules:

| Code | Severity |
| --- | --- |
| `LOCATION_CHANGED_WITHOUT_TRANSITION` | warning |
| `CHARACTER_APPEARED_WITHOUT_ENTER` | error |
| `CHARACTER_DISAPPEARED_WITHOUT_EXIT` | error |
| `WEATHER_CHANGED_WITHOUT_EVIDENCE` | warning |
| `TIME_DISCONTINUITY` | warning |
| `POV_CHANGED_WITHOUT_BOUNDARY` | warning |
| `META_TEXT_MISMATCH` | error |
| `INTENTIONAL_SCENE_RESET` | info |

Metadata evidence is checked against the block text's grapheme spans. Explicit
location, weather, and time changes require matching evidence unless the rule's
transition policy permits the change. POV changes inside a scene require a
transition block. Presence changes inside a block must be explained by ordered
enter or exit events.

## Scene Transitions

A `continuous` scene compares the preceding scene's `state_out` with its own
`state_in`. Comparable location, weather, time, POV, and character-presence
differences produce the corresponding finding. Values marked `unknown` or
`not_applicable` are deliberately excluded from comparisons.

`opening`, `cut`, `time_skip`, `flashback`, and `parallel` are reset boundaries.
They may change seeded state without explicit character exit or enter events.
When comparable state actually changes, the detector emits one informational
`INTENTIONAL_SCENE_RESET` finding that records the mode and changed state; it
does not emit character appearance or disappearance errors for that boundary.

## HTTP Boundary

`POST /v1/novels/{novelId}/detector-runs` requires `novel:analyze`,
`Idempotency-Key`, and a strong revision `If-Match`. The request's
`revision_hash` must equal `If-Match`. A successful response returns the same
hash as `ETag`; stale hashes return `412` without running analysis.

Golden coverage for every code and transition mode lives in
`modules/detector/src/test/resources/golden/detector-cases.json`.

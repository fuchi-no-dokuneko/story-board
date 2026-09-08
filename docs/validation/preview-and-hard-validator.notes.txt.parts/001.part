# Preview and deterministic hard validation

ADR-296 adds a side-effect-free preview path over the immutable editing core.
Callers supply the candidate revision ID and timestamp, so preview never reads the
clock. New block-version IDs are derived deterministically from the operation and
stable block IDs. Repeating the same request therefore produces the same canonical
candidate hash and render bytes.

A successful preview returns the base revision ID/hash, normalized typed operation,
candidate hash, block and scene-seed diff, render packet, violations, warnings, and
committability. `extend_block` normalizes to `replace_block_range`. Preview has no
storage writer or head-update dependency and cannot mutate canonical history.

Text that cannot enter the canonical model still returns a deterministic proposal
fingerprint and structured violations; its diff and render packet are unavailable.
Canonical candidates with metadata violations retain their diff and render packet so
the caller can inspect and correct them.

The validator emits the ten stable section 7.3 codes. Text issues include grapheme
and sentence counts plus safe split anchors. Evidence uses end-exclusive grapheme
offsets and an exact SHA-256 quote hash. Presence is resolved from the scene seed and
ordered local events. Unknown values cannot become explicit through extractor/model
metadata without a current same-block evidence span.

`modules/validator/src/test/resources/golden/validator-cases.json` is the acceptance
matrix: every documented code has one passing and one failing case.

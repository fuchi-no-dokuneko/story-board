# Deterministic Rendering and Metadata Resolution

The renderer is a pure function over an immutable revision, its authoritative
SHA-256 content hash, the fixed renderer version, and an inclusive block range.
It does not read time, network, storage, or other mutable state.

## Text and offsets

Blocks are emitted in canonical chapter, scene, and block order. One LF (`\n`)
separates adjacent blocks, including blocks on opposite scene boundaries. The
offset map uses zero-based, half-open Unicode code-point offsets into
`rendered_text`; UTF-8 bytes and Java UTF-16 code units are not offset units.
Each block, resolved metadata entry, and offset entry has the same block ID at
the same list position.

A subrange is inclusive. Its first offset is zero, but metadata `before` is
resolved from the complete preceding scene history. Its `scene_boundaries`
contains every scene from the first selected block's scene through the last,
including empty scenes between them. An all-range render includes boundaries
for empty scenes even when the revision has no blocks.

## State machine

Each scene starts independently. Missing `time`, `location`, `weather`, and
`pov` values resolve to `{"mode":"unknown"}`; presence starts from the sorted,
unique `present_character_ids` seed or an empty set.

- `explicit` replaces the resolved value. It accepts the standard `value`
  member and the specification's inline POV shape.
- `inherited` retains the exact prior resolved value. At scene start this means
  unknown, never a value from the previous scene.
- `unknown` replaces the value with an unknown marker and is never inferred.
- `not_applicable` replaces the value with its marker so detectors can omit it
  from transition comparisons.
- Presence events remain in canonical input order; `enter` and `exit` update a
  sorted resolved character set.

Every block records snapshots as `before`, ordered `events`, and `after`. Every
scene records its seed-derived `state_in` and final `state_out`; an empty scene
has identical boundary states. Malformed observations and presence events are
rejected instead of being interpreted as facts.

## Wire packet

`RenderPacket.canonicalValue()` is the transport projection. It uses snake-case
keys and scalar typed IDs and includes `revision_hash`, `renderer_version`,
`range`, `rendered_text`, `blocks`, `resolved_meta`, `offset_map`, and
`scene_boundaries`. Serializing this value with canonical JSON produces
byte-identical output for identical inputs. The golden state-machine fixture is
`modules/renderer/src/test/resources/golden/renderer-state-machine.json`.

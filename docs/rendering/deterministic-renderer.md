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

An image block remains part of this sequence. Its normalized caption is emitted
into `rendered_text` exactly like another block's text, so offsets and compiled
TXT remain unambiguous. Its rendered-block entry additionally carries the
immutable artifact ID, content SHA-256, media type, pixel dimensions, and alt
text. Render packets never inline the binary image.

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

## Image blocks

Canonical revisions store the reserved `storyblock.image` descriptor in block
extensions; API edit drafts and render packets expose it as the typed `image`
member. Callers cannot smuggle that reserved key through generic extensions.
Image blocks support insert, replace, move, and delete operations. Text-only
split, merge, and extend operations reject any source or replacement image so a
binary reference is never silently discarded or duplicated.

The descriptor accepts PNG or JPEG and binds an immutable portable artifact by
novel, artifact ID, content SHA-256, detected dimensions, media type, and alt
text. Commit and PDF rendering independently recheck those bindings.

## PDF rendering

`pdf-renderer-1.0.0` lays out one immutable full revision as A4 pages. It emits
a cover, chapter and optional scene headings, wrapped body paragraphs, scaled
illustrations, captions, and page numbers. Each page is deterministically
rasterized and encoded into a PDF 1.4 document using fixed layout, JPEG settings,
object order, and metadata-free bytes. The renderer reads no time or network
state; image bytes arrive through a resolver that must satisfy the canonical
descriptor hash and dimensions.

The REST response reports the renderer version plus exact page and image counts.
Two renders of the same revision and portable artifacts must be byte-identical;
the renderer and API suites assert that property.

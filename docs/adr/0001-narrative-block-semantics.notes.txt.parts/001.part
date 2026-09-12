# ADR 0001: Narrative Block Semantics

- Status: Accepted
- Date: 2026-08-21
- Owner: StoryBlock local implementation

## Decision

Canonical content is organized as novel, chapter, scene, and immutable block
versions. A live block contains one or two complete sentences, no more than 100
Unicode extended grapheme clusters, and at most one direct speaker. Presence
events are local deltas and require evidence in the same block version.

A block may additionally reference one immutable PNG or JPEG portable artifact.
The canonical reference is the reserved `storyblock.image` extension containing
artifact identity, SHA-256, media type, decoded dimensions, and alt text; its
block text is the compiled-text caption. Image blocks retain ordinary stable
block identity and may be inserted, replaced, moved, or deleted. Operations
whose meaning is text-only—split, merge, and extend—reject image blocks.

Stable block identity survives movement and one-to-one rewriting. Any canonical
text or metadata change creates a new block-version identity. Derived renderer,
detector, monitor, and style outputs never become canonical implicitly.

## Consequences

Generic JSON patching is prohibited. Every write is represented by a typed edit
operation and validated against the selected immutable base revision.

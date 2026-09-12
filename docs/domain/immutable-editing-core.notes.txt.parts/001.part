# Immutable editing core

ADR-295 implements the canonical `Novel -> Chapter -> Scene -> Block` hierarchy as
immutable Java records. `RevisionManifest` rejects duplicate chapter, scene, block,
and block-version identities and derives one selected block version for every live
block. Scene boundaries remain derived data and are not serialized into canon.

The public edit model is the sealed `EditOperation` interface. Its only permitted
payloads are the ten operations in specification section 6.2; no generic JSON Patch
or raw-path mutation type exists.

Range operations capture:

- the scene ID and ordered block/version pairs;
- the immediately preceding and following stable block IDs;
- a SHA-256 hash over the length-prefixed ordered identity/version sequence.

Validation checks all four values against the immutable base revision. Cross-scene
moves additionally capture both complete scene boundary contracts. Split and merge
operations expose source-version-to-result-block provenance mappings.

`NarrativeEditor` applies a validated operation in memory and returns a new revision.
Moves preserve block and version IDs. Text or canonical metadata changes materialize
a new block version. Restore selects a historical manifest's exact content in a new
child revision; it never mutates or deletes the historical revision.

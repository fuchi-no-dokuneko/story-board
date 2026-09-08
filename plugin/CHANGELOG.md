# Changelog

All notable changes to this standalone skill are recorded here.

## 1.1.0 - 2026-08-29

- Added first-class editable image-block contracts backed by portable immutable PNG/JPEG artifacts.
- Added raw binary `upload-image` and deterministic `render-pdf` commands with private, no-clobber output handling.
- Added five-character visual-reference validation: one initial image and 2–6 plain-background variants per identity.
- Expanded the catalog to 39 routes and 140 DTO schemas, with executable binary transport and workflow coverage.

## 1.0.0 - 2026-08-27

- Rebuilt the skill from StoryBlock controllers, application/domain contracts, canonical schemas, security filters, tests, fixtures, and runtime configuration.
- Added a 37-route endpoint manifest and generated human-readable catalog.
- Added 135 draft 2020-12 DTO schemas with provenance, confidence, and validated examples.
- Added a zero-dependency HTTPS client, strict JSON parser, code-matched sentence and schema validation, typed UUIDv7 helpers, problem parser, media-aware generic endpoint caller, and high-level authoring workflows.
- Added complete bulk registration, persistence verification, incremental preview/commit, render, export, job, and permission-hardened artifact commands.
- Added standalone metadata, examples, unit/contract/CLI tests, authentication guidance, workflow documentation, and error guidance.
- Marked unresolved preview serialization, Actuator response details, and rewrite-proposal read isolation explicitly as open questions.

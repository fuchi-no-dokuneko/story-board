# StoryBlock agent workflows

These workflows are derived from controller code, DTOs, filters, and HTTP tests. Consult `endpoints.json` and the referenced schema before changing any request shape.

## Bulk registration

Use bulk registration when the complete manuscript exists outside StoryBlock and should become a new canonical novel.

1. Create one `nov_` RFC 9562 UUIDv7 and one canonical UTC `created_at`. Do not refresh either on retry.
2. Build `AgentNovelRegistrationRequest` with exactly five unique main-character names, at least one chapter, and an exact positive Han count across chapter text.
3. Run local validation.
4. `register` sends `POST /v1/agent/novels`, `If-Match: *`, and `Idempotency-Key`. It accepts HTTP 201 for a new registration and HTTP 200 for an idempotent replay.
5. Preserve the complete source file and the emitted key.
6. Run `verify`. It reads `GET /v1/novels/{novelId}` and the returned head revision, reconstructs persisted block text, and compares identity, timestamp, Han sequence/count/digest, title, language, five names, zombie/TNT counts, and registration metadata.

```bash
node scripts/storyblock-author.mjs validate \
  --dto AgentNovelRegistrationRequest --file manuscript.json
node scripts/storyblock-author.mjs register --source manuscript.json --json
node scripts/storyblock-author.mjs verify --source manuscript.json --json
```

If a request times out after sending, retry the exact same payload and idempotency key. Do not create a replacement novel ID.

## Incremental edit

Use incremental authoring to change a novel that already has a canonical head.

1. Read the novel head and current revision with `read`.
2. Select one of the ten exact operation variants in `OperationEnvelope`.
3. Populate live revision/scene/block/block-version IDs and range hashes from the canonical revision. New operation, candidate revision, and block IDs must be typed UUIDv7 values.
4. Set `expected_head_hash` to the unquoted `sha256:...` head hash inside the body. The high-level commands turn it into the quoted strong `If-Match` header.
5. Reuse the body's `idempotency_key` as the HTTP header. The high-level commands do this automatically.
6. Preview, inspect, then commit the byte-equivalent JSON value without changing candidate identity or time.

The operation variants are:

- `insert_blocks`
- `replace_block_range`
- `delete_block_range`
- `split_block`
- `merge_blocks`
- `extend_block`
- `move_block_range`
- `correct_block_meta`
- `set_scene_initial_meta`
- `restore_revision_content`

Use `dtos --json` or the individual schema files to inspect payload guards. Blocks must respect sentence and 100-grapheme constraints enforced by the domain.

### Image-block edit

1. Read the current head, then run `upload-image` with a stable image file and key. The command sends the file as raw PNG/JPEG bytes and returns a validated `block_image` descriptor.
2. Copy that descriptor into `BlockDraft.image`; write one or two complete sentences in `BlockDraft.text` as the caption.
3. Insert or replace the image block through the same preview/commit flow as text. Move and delete also work. Do not use split, merge, or extend on an image block.
4. For character art, validate `CharacterImageReferenceConfig`. It requires exactly five identities, one initial reference per identity, 2–6 variants, and a plain background for every asset.

Portable image artifacts are included automatically when a canonical package is exported. Never embed image bytes in an edit operation or character-reference config.

## Preview before commit

```bash
node scripts/storyblock-author.mjs preview-edit \
  --novel-id nov_UUIDV7 --file request.json --json
node scripts/storyblock-author.mjs commit \
  --novel-id nov_UUIDV7 --file request.json --json
```

Preview is deterministic and does not move the head. Reject a preview with violations. Warnings are returned separately and should be reviewed. Commit can still fail with `412 REVISION_CONFLICT` if another commit moved the head, or `422 DETERMINISTIC_VALIDATION_FAILED` if the operation is invalid. After a head conflict, do not merely replace the hash: read the new revision and rebuild all guards as a new operation.

Undo is a preview-only operation at `novels.undo-previews.create`. Call it generically with an `UndoPreviewRequest`; use the returned restoration operation through the normal preview/commit contract only as justified by its response.

## Render

`render` validates `RenderRequest`, retrieves the referenced revision to obtain its hash, and sends `POST /v1/novels/{novelId}/renders` with the correct precondition. Omit or set both block bounds to `null` for the complete revision; use real block IDs for a bounded render.

```bash
node scripts/storyblock-author.mjs render \
  --novel-id nov_UUIDV7 --file render-request.json --json
```

The result is a structured `RenderPacket` with rendered blocks and offsets. Do not treat rendered text as a new canonical revision.

For a binary, deterministic A4 document, render the same immutable full revision through the PDF route:

```bash
node scripts/storyblock-author.mjs render-pdf \
  --novel-id nov_UUIDV7 --file pdf-render-request.json \
  --output novel.pdf --json
```

The command reports exact byte, page, and embedded-image counts from response metadata. It writes mode `0600` and does not overwrite an existing file unless `--force` is explicit.

## Export and import

Export is asynchronous. `export` resolves the current head unless `--revision-id` is given, sends `ExportRequest`, and returns a job reference. Poll the returned `job_id`, then download the resulting `artifact_id`.

```bash
node scripts/storyblock-author.mjs export --novel-id nov_UUIDV7 \
  --format canonical-package --json
node scripts/storyblock-author.mjs job --job-id job_UUIDV7 --json
node scripts/storyblock-author.mjs artifact --artifact-id art_UUIDV7 \
  --output novel-package.json
```

Import accepts `ImportRequest` at `POST /v1/imports`; use generic `call imports.create` with wildcard `If-Match` and a stable idempotency key. Validate the canonical document against `CanonicalRevision` or `CanonicalPackage` first. HTTP 200 is replay; HTTP 201 is a new import.

## Failure diagnosis

1. Re-run the failing command with `--json` and retain `request_id` for log correlation.
2. Distinguish local exits 2/3/7 from network exit 6 and server exits 4/5.
3. For 401, check whether the key is missing, malformed, expired, revoked, or for another server. For 403, compare endpoint scopes with the credential scopes.
4. A default 404 may conceal a cross-novel denial; do not probe other novel IDs.
5. For 409 idempotency conflicts, never reuse a key with a changed body.
6. For 412, read the current head and reconstruct revision-bound guards.
7. For 422, inspect `violations`, correct the operation, and use a new operation identity/key.
8. For 429, honor `Retry-After` and retry the unchanged operation.
9. For 5xx/network uncertainty after a mutation, retry only the unchanged idempotent request.

See `error-model.md` for the full response fields and known status semantics.

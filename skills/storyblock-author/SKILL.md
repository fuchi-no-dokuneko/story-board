---
name: storyblock-author
description: Discover and operate the complete StoryBlock self-signed HTTPS API, validate its DTOs offline, register or verify complete manuscripts, upload reference art, edit first-class image blocks, render deterministic PDFs, transfer canonical data, and diagnose problem responses. Use for autonomous StoryBlock authoring or API inspection.
---

# StoryBlock Author

Operate StoryBlock with the zero-dependency Node.js CLI in this folder. It is standalone: do not read repository source at runtime and do not guess fields that are absent from the bundled manifests.

## Operating rules

- Run commands from this skill directory, or invoke `scripts/storyblock-author.mjs` by its full path.
- Connect only over HTTPS. The client deliberately accepts StoryBlock's locally generated self-signed certificate and uses IPv4.
- The default is `https://127.0.0.1:8443`. Keep the server on loopback; for remote use, connect through a private SSH tunnel and keep the same local URL.
- Never request, upload, replace, or provision TLS certificate/key material. Never introduce a reverse proxy or public certificate flow.
- Treat IDs, timestamps, hashes, revision heads, and idempotency keys as immutable retry inputs. A changed payload is a new operation.
- Validate locally before every mutation. Preview an incremental edit before committing the exact same request.
- Do not print or persist an access key. Pass it with `STORYBLOCK_ACCESS_KEY` or `--access-key`.

## How does an agent submit written novel content?

StoryBlock supports two distinct modes:

1. **Bulk submission:** send a complete manuscript with `POST /v1/agent/novels`. Use `register --source manuscript.json`, then `verify --source manuscript.json`. This is the simplest path for a finished novel and creates the initial canonical revision.
2. **Incremental authoring:** start from an existing novel and submit one verified edit operation through `POST /v1/novels/{novelId}/edit-previews`, then commit the unchanged request through `POST /v1/novels/{novelId}/commits`. Use `preview-edit`, inspect violations/warnings, then `commit` only if the head is unchanged.

Bulk registration is not a shortcut for modifying an existing manuscript. Incremental commit is not a complete-manuscript upload.

## Configuration and authentication

The CLI recognizes:

- `STORYBLOCK_BASE_URL`: HTTPS origin; default `https://127.0.0.1:8443`.
- `STORYBLOCK_ACCESS_KEY`: bearer owner token or scoped access-key secret; omit only for public routes or a trusted-LAN server.
- `STORYBLOCK_TIMEOUT_MS`: integer from 1 through 300000; default 15000.
- `STORYBLOCK_USER_AGENT`: non-empty request user agent; default `storyblock-author/1.1.0`.

Equivalent overrides are `--base-url`, `--access-key`, `--timeout-ms`, and `--user-agent`, placed after the command. Protected requests use `Authorization: Bearer <credential>`. Trusted-LAN mode authenticates every client able to reach the port as the owner; no key or manual client approval is used in that mode. See [references/authentication.md](references/authentication.md) before selecting a scope.

For a server on another host, keep its loopback bind and use a private tunnel:

```bash
ssh -N -L 8443:127.0.0.1:8443 operator@private-host
node scripts/storyblock-author.mjs health
```

## Discover endpoints and DTOs offline

```bash
node scripts/storyblock-author.mjs endpoints
node scripts/storyblock-author.mjs endpoints --json
node scripts/storyblock-author.mjs describe agent.novels.register --json
node scripts/storyblock-author.mjs dtos
node scripts/storyblock-author.mjs dtos --json
node scripts/storyblock-author.mjs validate --dto ApiProblem --file examples/api-problem.json
```

`endpoints` reads [references/endpoints.json](references/endpoints.json). `dtos` and `validate` read the draft 2020-12 schemas in `references/dtos/`. Unknown endpoint or DTO names exit 7.

## Bulk registration and persistence verification

1. Copy `examples/minimal-novel.json` and replace its values. Generate a typed ID with `node scripts/storyblock-author.mjs id --prefix nov`.
2. Preserve exactly nine top-level fields. Use exactly five distinct main-character names. `expected_han_characters` must equal the NFC-normalized Unicode Han code-point count across chapter text only.
3. Use a canonical Java `Instant.toString()` UTC timestamp: seconds with `Z`, or a non-zero 3/6/9-digit fractional part.
4. Validate, register, and independently verify persistence:

```bash
node scripts/storyblock-author.mjs validate \
  --dto AgentNovelRegistrationRequest --file manuscript.json
node scripts/storyblock-author.mjs register --source manuscript.json --json
node scripts/storyblock-author.mjs verify --source manuscript.json --json
```

`register` supplies `If-Match: *` and a deterministic idempotency key derived from the complete payload unless `--idempotency-key` is supplied. A retry must use the same file and key. Registration success alone is not persistence proof; claim completion only when `verify` returns `"ok": true`.

## Incremental preview and commit

Read the current head and canonical revision first:

```bash
node scripts/storyblock-author.mjs read --novel-id nov_UUIDV7 --json
```

Build an `EditPreviewRequest` using the current revision ID/hash and live scene/block/version IDs. Supported operation discriminators are `insert_blocks`, `replace_block_range`, `delete_block_range`, `split_block`, `merge_blocks`, `extend_block`, `move_block_range`, `correct_block_meta`, `set_scene_initial_meta`, and `restore_revision_content`. Inspect their exact schemas with `dtos`; never infer a generic patch shape.

```bash
node scripts/storyblock-author.mjs validate \
  --dto EditPreviewRequest --file examples/preview-request.json
node scripts/storyblock-author.mjs preview-edit \
  --novel-id nov_UUIDV7 --file preview-request.json --json
node scripts/storyblock-author.mjs commit \
  --novel-id nov_UUIDV7 --file preview-request.json --json
```

The high-level commands bind the operation idempotency key to `Idempotency-Key` and quote its expected head hash for `If-Match`. Do not commit when preview reports violations, and do not alter the previewed operation, candidate revision ID, or candidate timestamp. If the head changes, read again and construct a new operation. Use generic `call novels.undo-previews.create` for an undo preview; its exact request is `UndoPreviewRequest`.

## Image blocks and character references

An image is an editable narrative block, not a base64 field inside text. Upload immutable PNG/JPEG bytes first, then put the returned `block_image` descriptor in `BlockDraft.image`; `BlockDraft.text` is its visible caption. Image blocks support insert, replace, move, and delete. They deliberately reject split, merge, and extend because those operations have no deterministic binary meaning.

```bash
node scripts/storyblock-author.mjs upload-image \
  --novel-id nov_UUIDV7 --file character.png \
  --alt-text 'Plain-background reference portrait of the character.' --json
node scripts/storyblock-author.mjs validate \
  --dto BlockDraft --file examples/image-block-draft.json --json
```

The upload command sends raw bytes, never JSON/base64. Inputs must be PNG or JPEG, 1–1,500,000 bytes, with dimensions no larger than 8192×8192 and no more than 40 million pixels. Uploaded artifacts are immutable, novel-bound, content-hashed, and included in canonical-package transfer.

Before later illustration work, maintain a `CharacterImageReferenceConfig`: exactly five distinct character identities, each with one `initial` image and 2–6 reference variants. Every reference must use a `plain` background; preserve the `identity_lock` across variants. Validate that DTO before using its descriptors as later image-block references.

## Render, PDF, export, import, jobs, and artifacts

```bash
node scripts/storyblock-author.mjs render \
  --novel-id nov_UUIDV7 --file examples/render-request.json --json
node scripts/storyblock-author.mjs render-pdf \
  --novel-id nov_UUIDV7 --file examples/pdf-render-request.json \
  --output novel.pdf --json
node scripts/storyblock-author.mjs export \
  --novel-id nov_UUIDV7 --format canonical-revision --json
node scripts/storyblock-author.mjs job --job-id job_UUIDV7 --json
node scripts/storyblock-author.mjs artifact \
  --artifact-id art_UUIDV7 --output export.json
```

`render`, `render-pdf`, and `export` fetch the referenced canonical revision first and set its strong ETag. PDF rendering is synchronous through `POST /v1/novels/{novelId}/pdf-renders`; it lays out complete text, image blocks, and captions in deterministic A4 bytes. `render-pdf` and `artifact` create mode-0600 output and refuse to overwrite a file without `--force`. Import is available through the verified generic endpoint:

```bash
node scripts/storyblock-author.mjs call imports.create \
  --params import-params.json --body import-request.json --json
```

The params file must provide `If-Match: *` and `Idempotency-Key` under `headers`. Validate `ImportRequest` before sending. See [references/workflows.md](references/workflows.md) for complete sequences.

## Generic endpoint calls

Use `call` for every cataloged route without a high-level command:

```json
{
  "path": {"novelId": "nov_UUIDV7"},
  "query": {"limit": 50},
  "headers": {
    "If-Match": "\"sha256:HEX64\"",
    "Idempotency-Key": "stable-operation-key"
  }
}
```

```bash
node scripts/storyblock-author.mjs describe novels.monitor-runs.create
node scripts/storyblock-author.mjs validate --dto MonitorSubmissionRequest --file body.json
node scripts/storyblock-author.mjs call novels.monitor-runs.create \
  --params params.json --body body.json --json
```

The CLI validates declared path/query values, required mutation headers, and request DTOs before network I/O.

## Failure diagnosis and exit codes

Use `--json` for stable machine-readable output. API failures expose HTTP status, problem type/title/code/detail/instance, request ID, validation issues when present, and `Retry-After` when sent. Read [references/error-model.md](references/error-model.md).

- `0`: success.
- `1`: unexpected local failure.
- `2`: invalid command or option usage.
- `3`: local DTO validation or persistence verification failure.
- `4`: HTTP 401 or 403 authentication/authorization failure.
- `5`: other non-2xx API response.
- `6`: connection, timeout, TLS, or response-transport failure.
- `7`: unknown endpoint or DTO.

Common failures are missing/invalid `If-Match` or `Idempotency-Key`, stale head (`412`), replaying a key with changed content (`409`), deterministic edit rejection (`422`), expired artifacts (`410`), missing scope (`403`), hidden cross-novel access (`404` by default), payloads over 2 MiB (`413`), and rate limit exhaustion (`429`, normally `Retry-After: 60`). Retry a mutation only with its exact original body and idempotency key.

## Reference routing

- [references/workflows.md](references/workflows.md): operational sequences and retry invariants.
- [references/authentication.md](references/authentication.md): credentials, scopes, owner/trusted-LAN behavior, and novel isolation.
- [references/error-model.md](references/error-model.md): problem details, statuses, and retry policy.
- [references/endpoints.md](references/endpoints.md): all endpoint contracts and source provenance.
- [references/dtos.md](references/dtos.md): all DTO fields, constraints, confidence, and schema links.
- [examples/README.md](examples/README.md): which placeholders must come from a live revision.

`PreviewResponse` nested Java-record serialization and Spring Actuator response details are explicitly marked as open questions. `rewrite-proposals.read` has a code-visible authorization-boundary question; do not assume its current cross-novel behavior is intentional.

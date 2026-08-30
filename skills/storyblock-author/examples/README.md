# StoryBlock examples

`minimal-novel.json` is ready for offline validation. Generate a fresh `nov_` UUIDv7 before registering it against a persistent server so the example does not collide with an earlier run.

The edit, preview, commit, and render files are structurally valid templates. Their revision, scene, block, block-version, and hash values are illustrative. Replace them with values from the current novel head and canonical revision before making a live request. Reuse an operation's exact body and idempotency key when retrying it.

`image-block-draft.json` shows the object copied into an insert/replace operation after `upload-image`; replace all artifact fields with that command's returned `block_image`. The text is the editable caption. `pdf-render-request.json` selects the immutable revision rendered by `render-pdf`.

The embedded example in `references/dtos/CharacterImageReferenceConfig.schema.json` is a complete five-character reference template. Each identity has one initial image and two plain-background variants; live configs may contain up to six variants.

`edit-operation-update.json` uses the verified `replace_block_range` operation. There is no separate generic “update” operation. Validate any file before sending it:

```bash
node scripts/storyblock-author.mjs validate --dto EditPreviewRequest --file examples/preview-request.json
node scripts/storyblock-author.mjs validate --dto BlockDraft --file examples/image-block-draft.json
node scripts/storyblock-author.mjs validate --dto PdfRenderRequest --file examples/pdf-render-request.json
```

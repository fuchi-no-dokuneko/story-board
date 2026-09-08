# StoryBlock examples

`minimal-novel.json` is ready for offline validation. Generate a fresh `nov_` UUIDv7 before registering it against a persistent server so the example does not collide with an earlier run.

The edit, preview, commit, and render files are structurally valid templates. Their revision, scene, block, block-version, and hash values are illustrative. Replace them with values from the current novel head and canonical revision before making a live request. Reuse an operation's exact body and idempotency key when retrying it.

`edit-operation-update.json` uses the verified `replace_block_range` operation. There is no separate generic “update” operation. Validate any file before sending it:

```bash
node scripts/storyblock-author.mjs validate --dto EditPreviewRequest --file examples/preview-request.json
```

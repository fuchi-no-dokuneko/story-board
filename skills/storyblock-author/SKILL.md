---
name: storyblock-author
description: Operate StoryBlock's self-signed HTTPS authoring API to check health, list or read novels, register a manuscript, and verify persisted Han text and aggregate counts. Use for StoryBlock novel registration and verification.
---

# StoryBlock Author

Use the deterministic helper from the repository root:

```bash
node skills/storyblock-author/scripts/storyblock-author.mjs health
node skills/storyblock-author/scripts/storyblock-author.mjs list --page 0 --size 50 --query ""
node skills/storyblock-author/scripts/storyblock-author.mjs register --source manuscript.json
node skills/storyblock-author/scripts/storyblock-author.mjs read --novel-id NOVEL_ID
node skills/storyblock-author/scripts/storyblock-author.mjs verify --source manuscript.json
```

The default service is `https://127.0.0.1:8443`; override it with `--base-url` or `STORYBLOCK_BASE_URL`. The helper accepts self-signed HTTPS endpoints and forces IPv4. For a public host, use the normal authenticated server profile, restrict ingress with a firewall allowlist, and provide the owner token with `--token-file` or `STORYBLOCK_TOKEN_FILE`. The helper automatically reads the repo-local `.local/storyblock/secrets/owner-token` when present and never prints the token.

Read [references/schema.md](references/schema.md) before preparing a manuscript or interpreting verification output. `register` is the only mutating command; use it only when novel registration is requested. The source must already contain its typed UUIDv7 `novel_id` and canonical UTC `created_at`; the helper never generates or replaces them.

Registration snapshots the exact validated payload and derives its default idempotency key from that canonical payload's SHA-256. Retry with the same source values and key; never refresh `novel_id`, `created_at`, or any manuscript field during a retry. Supplying a changed payload is a new registration attempt, not a retry.

For the specified 10,000-character Minecraft E2E manuscript, pass `--profile minecraft-10k` to both `register` and `verify`. This enforces exactly five unique main-character names, 10,000 Han code points in chapter text, 1,000 zombies, and 1,000 TNT cannons. A successful registration is not proof of persistence; claim success only after `verify` returns `"ok": true`.

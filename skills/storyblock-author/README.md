# StoryBlock Author 1.0.0

A standalone, zero-runtime-dependency Node.js skill for discovering and operating the complete StoryBlock HTTPS API. It bundles a code-verified 37-endpoint manifest, 135 DTO schemas, offline validation, high-level authoring commands, generic endpoint calls, examples, and tests.

## Requirements

- Node.js 20 or newer.
- A reachable StoryBlock HTTPS API for online commands.
- A bearer credential for protected routes, unless the server runs in trusted-LAN mode.

No package installation is needed:

```bash
node scripts/storyblock-author.mjs --help
node scripts/storyblock-author.mjs endpoints --json
node scripts/storyblock-author.mjs validate \
  --dto AgentNovelRegistrationRequest --file examples/minimal-novel.json
```

The folder can be copied by itself. Every runtime import and reference resolves within this directory, and `package.json` declares no dependencies.

## Connection model

The default API is `https://127.0.0.1:8443`. StoryBlock terminates HTTPS itself with a locally generated self-signed leaf; the client accepts that certificate and rejects plain HTTP and IPv6 URLs. Keep the API on loopback. For remote access, forward it privately:

```bash
ssh -N -L 8443:127.0.0.1:8443 operator@private-host
```

The client never asks for or installs TLS material. Configure it with:

```bash
export STORYBLOCK_BASE_URL=https://127.0.0.1:8443
export STORYBLOCK_ACCESS_KEY='credential-issued-by-storyblock'
export STORYBLOCK_TIMEOUT_MS=15000
export STORYBLOCK_USER_AGENT='my-authoring-agent/1.0'
```

Avoid command-line credentials when process arguments may be observable. Public `health` and OpenAPI routes need no credential. Admin catalog routes require the owner/operator identity; ordinary novel reads require `novel:read`.

## Submit content

For a complete manuscript:

```bash
node scripts/storyblock-author.mjs register --source manuscript.json --json
node scripts/storyblock-author.mjs verify --source manuscript.json --json
```

For an existing novel, preview then commit an unchanged operation:

```bash
node scripts/storyblock-author.mjs preview-edit \
  --novel-id nov_UUIDV7 --file edit-request.json --json
node scripts/storyblock-author.mjs commit \
  --novel-id nov_UUIDV7 --file edit-request.json --json
```

See [SKILL.md](SKILL.md) for agent instructions and [references/workflows.md](references/workflows.md) for complete workflows.

## Discovery and generic calls

```bash
node scripts/storyblock-author.mjs endpoints
node scripts/storyblock-author.mjs describe rewrite-proposals.create --json
node scripts/storyblock-author.mjs dtos
node scripts/storyblock-author.mjs validate --dto RewriteProposalRequest --file request.json
node scripts/storyblock-author.mjs call rewrite-proposals.create \
  --params params.json --body request.json --json
```

`references/endpoints.json` is the stable endpoint manifest. Each `references/dtos/*.schema.json` file uses JSON Schema draft 2020-12 and includes source provenance, confidence, and a validated example.

## Development

```bash
npm test
npm run smoke
npm run docs:check
```

Tests use Node's built-in test runner and injected transports; no live server is required. `npm run docs` regenerates the two human-readable catalogs only from bundled machine-readable files.

## Security notes

- Self-signed certificate verification is deliberately disabled for this private StoryBlock client. Do not use it for unrelated services.
- Credentials are sent only as bearer authorization to the configured HTTPS origin and are never printed.
- Artifact output uses mode `0600` and refuses replacement unless `--force` is explicit.
- The generic client rejects HTTP, URL credentials, and IPv6 targets.

Open questions are never silently filled in. Search the catalogs for `OPEN QUESTION` before automating an uncertain response shape.

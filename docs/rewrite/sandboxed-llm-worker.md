# Sandboxed LLM Rewrite Worker

ADR-307 defines a one-shot process that turns one bounded immutable input into
one text-only proposal. It cannot read the StoryBlock database, call an edit or
commit service, claim a broader tenant credential, or publish a canonical
revision.

## Capability Boundary

The process has three capabilities:

1. read one canonical `rewrite-input-1.0.0` object from standard input;
2. make one `POST` to the configured model gateway;
3. write one canonical `rewrite-proposal-1.0.0` object to standard output.

The worker module depends only on Spring Boot, canonical JSON, the core domain,
and `storyblock-rewrite`. Maven Enforcer rejects application, monitor, security,
storage API, and SQLite dependencies, and an ArchUnit rule rejects source-level
references to those packages or JDBC. The worker therefore has neither a
StoryBlock API credential nor a commit-capable class on its runtime classpath.
The trusted orchestration layer owns durable job claim and result persistence;
it passes only the immutable snapshot through this narrow process boundary.

## Bounded Input

The input binds a generated proposal ID to the exact analysis, novel, revision,
profile version, analyzer contract, window configuration, and finding hashes.
Each source block carries its block ID, block-version ID, exact text hash, text,
and an `editable` flag.

- At most 64 contiguous blocks are editable.
- At most two read-only context blocks may appear on either side.
- Every block must satisfy the canonical 100-grapheme block invariant.
- Findings and block identities must be unique.
- Typed constraints cap changed blocks and total output graphemes.
- One to 16 bounded style directives may be supplied.

The full object produces `input_hash`. The model sees only that hash, source
blocks, editability, and typed constraints. Novel, revision, profile, proposal,
and finding identities are not included in the model-visible payload.

## Model Protocol

The configured endpoint is a small provider adapter, not an unrestricted agent
tool endpoint. The request is canonical JSON with this fixed shape:

```text
protocol_version, model, instructions, input, response_schema, tools
```

`tools` is always an empty array. Fixed instructions label source text and style
directives as untrusted data, limit changes to editable blocks, and prohibit
external actions. The embedded response schema allows exactly:

```json
{
  "model": "configured-model-id",
  "output": {
    "input_hash": "sha256:...",
    "replacements": [
      {"block_id": "blk_...", "text": "Complete replacement sentence."}
    ]
  },
  "protocol_version": "rewrite-model-1.0.0"
}
```

Unknown fields, prose wrappers, tool calls, a different model or input hash,
duplicate or read-only block targets, unchanged text, invalid block text, and
output beyond the job limits all fail closed. Accepted replacements are bound
back to the supplied source block-version and text hashes. The final proposal
embeds the exact input, canonical model-response hash, candidate hashes,
creation time, and its own canonical proposal hash. It contains no edit
operation, preview token, commit token, or status transition.

## Network And Secret Controls

The model endpoint must use HTTPS, except that IPv4 loopback HTTP is allowed for
local testing. User info, query, fragment, and IPv6 literals are rejected. The
JVM forces the IPv4 stack. The HTTP client never follows
redirects, bypasses process-wide proxy settings, and performs no automatic retry.
Connect/request timeouts and a
1-KiB to 256-KiB response ceiling are mandatory. Only HTTP 200 JSON is parsed.

The model bearer token is accepted only as process configuration and is added
to the `Authorization` header by `HttpLlmModelTransport`. It is absent from the
structured request, model-visible input, output, exceptions, and settings
rendering. The transport rejects a request or response containing the exact
credential, preventing accidental prompt inclusion or gateway reflection.
Application logging is disabled because standard output is the proposal data
channel; the worker code never logs source text, prompts, or model responses.

## Configuration

Spring maps these environment variables to worker properties:

```text
STORYBLOCK_LLM_WORKER_ENABLED=true
STORYBLOCK_LLM_WORKER_MODEL_ENDPOINT=https://model-gateway.internal/v1/rewrite
STORYBLOCK_LLM_WORKER_MODEL_TOKEN=<secret-manager-injected-value>
STORYBLOCK_LLM_WORKER_MODEL_ID=approved-model-version
STORYBLOCK_LLM_WORKER_CONNECT_TIMEOUT=10s
STORYBLOCK_LLM_WORKER_REQUEST_TIMEOUT=2m
STORYBLOCK_LLM_WORKER_MAX_RESPONSE_BYTES=65536
```

Do not pass the token on the command line. Use an environment or secret-manager
injection available only to the worker OS account. A canonical job can be run
as follows after packaging:

```bash
java -Djava.net.preferIPv4Stack=true \
  -jar apps/llm-worker/target/storyblock-llm-worker-0.1.0-SNAPSHOT.jar \
  < rewrite-input.json > rewrite-proposal.json
```

Standard input may end with one line terminator; other leading, trailing, or
non-canonical JSON bytes are rejected. A failed model or contract check emits no
proposal and exits unsuccessfully. Proposal gating, preserved-fact and near-copy
checks, re-scoring, preview, durable storage, staleness, and human approval are
controller responsibilities in ADR-308 and ADR-309.

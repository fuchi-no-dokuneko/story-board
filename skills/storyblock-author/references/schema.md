# StoryBlock Authoring Contract

## Transport and endpoints

The default base URL is `https://127.0.0.1:8443`. Requests use IPv4 and accept the server's self-signed certificate; IPv6 and plain HTTP are rejected. Public hosts must use the normal authenticated server profile, firewall-restricted ingress, and an owner token selected with `--token-file` or `STORYBLOCK_TOKEN_FILE`. The helper reads `.local/storyblock/secrets/owner-token` automatically when present.

| Command | Request |
|---|---|
| `health` | `GET /actuator/health` |
| `list` | `GET /v1/admin/novels?page=0&size=50&q=` |
| `read` | `GET /v1/admin/novels/{novelId}` |
| `register` | `POST /v1/agent/novels` |

`register` sends `Content-Type: application/json`, `If-Match: *`, and `Idempotency-Key`. Its generated key and reported `payload_sha256` are derived from the complete canonical request body, including `novel_id` and `created_at`.

## Registration manuscript

The source file is UTF-8 JSON with exactly these fields:

```json
{
  "novel_id": "nov_018f0f5e-7b4a-7c00-8000-000000000001",
  "created_at": "2026-08-24T12:00:00Z",
  "title": "string",
  "language": "string",
  "main_characters": ["five", "unique", "non-empty", "character", "names"],
  "zombie_count": 1000,
  "tnt_cannon_count": 1000,
  "expected_han_characters": 10000,
  "chapters": [
    {"title": "string", "text": "string"}
  ]
}
```

`novel_id` must use the exact `nov_` type prefix followed by a standard textual RFC 9562 UUIDv7: version nibble `7` and RFC variant bits. The caller creates this value once; the helper does not generate IDs.

`created_at` must equal canonical Java `Instant.toString()` UTC text, including a terminal `Z`. Common accepted forms are `2026-08-24T12:00:00Z`, `2026-08-24T12:00:00.123Z`, `.123456Z`, and `.123456789Z`. Offsets, noncanonical fractional precision, invalid calendar values, and normalized-but-unequal forms are rejected.

`main_characters` means five protagonist names, not five Unicode code points. Names must also be unique after trimming and NFC normalization. Chapter array order is manuscript order.

Han length is calculated only from each chapter's `text`; `novel_id`, `created_at`, titles, and all other metadata are excluded. JavaScript `\p{Script=Han}` with Unicode mode selects Han code points, so one selected Unicode code point equals one Chinese character. Punctuation, whitespace, Latin text, digits, and emoji do not count. `expected_han_characters` must equal the calculated count.

### Retry invariant

The first attempt and every retry must reuse the same complete payload. Reuse the same source file and, when manually supplied, the same `--idempotency-key`. Do not generate a new UUID, replace the timestamp, or edit content between attempts. With the default key, identical source values produce identical canonical POST bytes, `payload_sha256`, and idempotency key; changing any field changes both hashes and represents a new attempt.

The optional `minecraft-10k` profile requires all of the following:

- `expected_han_characters` and calculated Han count: exactly `10000`
- `zombie_count`: exactly `1000`
- `tnt_cannon_count`: exactly `1000`
- exactly five unique main-character names

## Read and verification response

`read` expects a JSON object shaped as follows; additional fields are allowed:

```json
{
  "schema_version": "string",
  "novel": {
    "novel_id": "nov_018f0f5e-7b4a-7c00-8000-000000000001",
    "main_characters": ["..."],
    "zombie_count": 1000,
    "tnt_cannon_count": 1000,
    "han_character_count": 10000,
    "han_text_sha256": "sha256:..."
  },
  "revision": {
    "novel_id": "nov_018f0f5e-7b4a-7c00-8000-000000000001",
    "created_at": "2026-08-24T12:00:00Z",
    "chapters": [
      {
        "scenes": [
          {"blocks": [{"text": "string"}]}
        ]
      }
    ]
  }
}
```

`verify` reads `manuscript.novel_id` by default; an optional `--novel-id` must match it exactly. It concatenates persisted block text in chapter, scene, and block order and independently derives the source and persisted Han sequences. It compares the identity, creation instant, Han sequence, code-point count, SHA-256, main characters, and aggregate zombie/TNT metadata. The JSON report lists every check and exits nonzero when any check fails.

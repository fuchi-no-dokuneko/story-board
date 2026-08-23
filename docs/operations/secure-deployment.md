# Secure deployment topology

The production Compose topology exposes only Caddy on ports 80 and 443. Caddy
terminates TLS and forwards `/v1`, `/v1/*`, and the minimal health endpoint to
the API over an internal Docker network. The API has no host port and is the
only application service with the `storyblock-data` volume.

All application images run as UID 10001 on read-only root filesystems. The API,
style worker, and LLM worker receive credentials through Docker secret mounts;
the entrypoint reads the mounted file without printing it and removes the file
reference from the child environment. Worker profiles do not mount the database
or Docker socket. The LLM worker has neither canonical storage nor API commit
credentials.

Provide protected files outside this repository before rendering the Compose
configuration:

```text
STORYBLOCK_OWNER_TOKEN_FILE
STORYBLOCK_SERVER_PEPPER_FILE
STORYBLOCK_STYLE_WORKER_TOKEN_FILE
STORYBLOCK_LLM_MODEL_TOKEN_FILE
```

The encrypted backup destination must be an off-host versioned mount. Keep the
backup encryption key outside both the SQLite volume and backup destination, as
described in `backup-and-restore.md`.

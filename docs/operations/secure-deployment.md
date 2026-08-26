# Secure self-signed deployment

StoryBlock terminates HTTPS in the API process. There is no reverse proxy,
certificate upload, ACME flow, or external certificate issuer. The first start
generates a self-signed server leaf with `CA=false`; its private key never leaves
the local repository installation or the private container volume.

The default bind is `127.0.0.1:8443`. Keep that default and use an SSH tunnel for
remote access. A private-interface deployment may set all three values together:

```text
STORYBLOCK_BIND_ADDRESS=192.168.1.20
STORYBLOCK_HTTPS_PORT=8443
STORYBLOCK_TLS_HOST=192.168.1.20
```

Changing `STORYBLOCK_TLS_HOST` regenerates the self-signed leaf with a matching
DNS or IPv4 subject alternative name. No certificate or key needs to be supplied
by the operator. Browsers will show an untrusted-certificate warning because the
leaf has no issuer; this is expected. Never expose this deployment directly to
the public internet.

## Repository-local installation

The supported non-container installation writes only below
`.local/storyblock/` in the repository. It also redirects Maven, temporary, and
cache files into that directory.

```bash
./install.sh
./scripts/local-server.sh start
./scripts/local-server.sh status
./scripts/local-server.sh stop
```

The only host prerequisite is Java 21 with `java` and `keytool`. The installer
does not invoke `sudo`, require root, write to system directories, or upload TLS
material. The owner credential is generated at
`.local/storyblock/secrets/owner-token` and is never printed.

## Container installation

Run `./install.sh` once to create repository-local secret files, then use a
rootless container runtime if containers are desired:

```bash
docker compose build
docker compose up -d api
```

The API generates its private key in `storyblock-tls-private`. Workers receive
only a separate public trust store from `storyblock-tls-public`; they cannot read
the API private key. The API is the only application service with the
`storyblock-data` volume. Worker profiles do not mount the database or container
socket. The LLM worker has neither canonical storage nor API commit credentials.

The encrypted backup destination must be an off-host versioned mount. Keep the
backup encryption key outside both the SQLite volume and backup destination, as
described in `backup-and-restore.md`.

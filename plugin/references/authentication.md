# StoryBlock authentication and authorization

## Credential transport

Protected endpoints accept exactly the HTTP bearer scheme:

```text
Authorization: Bearer <credential>
```

Set `STORYBLOCK_ACCESS_KEY` or pass `--access-key`. The CLI does not inspect repository secret files, log the credential, place it in output, or request TLS key material. Prefer the environment variable because command arguments may be visible to other processes.

Two bearer credential classes exist:

- The server owner token has every scope and the `operator` role. The server configures it; it must contain at least 32 characters. Obtain it through the server's private operator process, not through this skill.
- A scoped novel key has the one-time secret form `nv_key_<UUIDv7>.<43-character-base64url-secret>`. An existing owner or `novel:admin` principal creates one with `POST /v1/novels/{novelId}/access-keys`. Store the returned secret when issued because StoryBlock does not expose it again. Revoke it with `DELETE /v1/access-keys/{keyId}`.

No credential may be hardcoded in examples or committed files.

## Public and trusted-LAN behavior

`GET /v1/openapi.yaml` and aggregate `GET /actuator/health` are public. Static browser assets are outside this skill's endpoint catalog.

When the server explicitly enables trusted-LAN mode, every request that can reach the listening port is authenticated as the owner. There is no client secret exchange and no manual client approval. Network reachability is therefore the entire trust boundary. Keep the default loopback bind and use a private tunnel for remote clients.

## Scopes and roles

The code-defined scopes are:

- `novel:read`: novel heads/revisions, renders, exports, jobs/artifacts, monitor packets, and other protected GET reads.
- `novel:analyze`: detector runs.
- `novel:propose`: edit and undo previews.
- `novel:commit`: commits.
- `novel:admin`: novel creation/import/agent registration and access-key issue/revoke.
- `style:analyze`: start style analyses.
- `style:admin`: create/version/transition style profiles.
- `rewrite:propose`: reserve rewrite proposals.
- `monitor:submit`: submit monitor output.
- `worker:execute`: claim internal jobs and submit worker results.

The owner also has the `operator` role, required for `/v1/admin/**`, component health, and metrics. Scopes are endpoint-specific; they do not imply one another in the authorization configuration.

Consult each entry in `endpoints.json` for the exact scope/role. A missing/invalid/expired/revoked bearer token returns 401. A valid principal without the required authority returns 403.

## Novel isolation

Scoped access keys are bound to one novel. The boundary filter resolves novel ownership from path IDs and from associated jobs, artifacts, analyses, and access keys. Cross-novel access is hidden as 404 by default; a server configuration can instead expose it as 403. Do not use the difference for resource discovery.

OPEN QUESTION: `GET /v1/rewrite-proposals/{proposalId}` requires `novel:read`, but current controller and boundary-filter code do not resolve `proposalId` back to the credential's novel. It appears possible for any novel-scoped read credential to fetch a known proposal ID. This skill documents the code as observed and does not endorse relying on that behavior.

## Audit behavior

Credential issue/revoke, imports/registrations, commits, exports, style changes, monitor submissions, and rewrite reservation paths construct audit context from the authenticated actor, request ID, remote address where applicable, and occurrence time. Audit records are persisted internally; no public audit-list endpoint was discovered. Do not expect audit details in successful response DTOs.

## Connection security

StoryBlock terminates inbound HTTPS in its own process with a locally generated self-signed server leaf (`CA=false`). This client intentionally sets certificate verification off for StoryBlock connections. It still requires `https://`, rejects URL-embedded credentials, and forces IPv4. Keep the server loopback-only and use a private SSH tunnel rather than adding ACME, a reverse proxy, certificate upload, or a public bind.

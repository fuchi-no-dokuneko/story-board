# ADR 0009: Application-terminated self-signed TLS

- Status: Accepted
- Date: 2026-08-26

## Decision

StoryBlock terminates inbound HTTPS in the API process with an automatically
generated self-signed server leaf. The leaf is explicitly not a certificate
authority. Its private key remains in repository-local state or a private
container volume. Operators are never required to upload a certificate or key.

The default deployment binds to loopback and is reached remotely through a
private tunnel. No reverse proxy, ACME client, public certificate issuer, or
external certificate-distribution dependency belongs in the deployment.

Workers that call the API receive only an exported public trust store. They do
not mount or read the server keystore.

## Consequences

- Browsers and generic clients show a trust warning unless configured for the
  generated public leaf.
- Hostname or IP changes regenerate the leaf and invalidate prior trust.
- Internet-facing deployment is unsupported.
- Build, runtime state, secrets, and TLS files may stay entirely inside the
  repository-local `.local/storyblock/` directory.

# Repository operating constraints

## TLS and installation policy

- Keep inbound HTTPS termination inside the StoryBlock API process.
- Generate a self-signed server leaf locally with `CA=false`; never add an ACME
  flow, public certificate issuer, reverse proxy, or certificate-upload step.
- Never require an operator to supply or upload TLS certificate/key material.
- Keep the default bind on loopback and document a private tunnel for remote use.
- Keep `install.sh` unprivileged and repository-local. It must not call `sudo`,
  require root, write to system or home directories, or place caches, secrets,
  certificates, databases, logs, or installed binaries outside this repository.
- Preserve architecture tests that enforce these constraints.

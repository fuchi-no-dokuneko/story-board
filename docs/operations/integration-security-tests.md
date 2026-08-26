# Integration, concurrency, and security verification

The automated suite uses real temporary SQLite databases and Spring MockMvc.
It covers concurrent readers and writers, WAL checkpointing, stale head CAS,
multi-novel isolation, scoped/expired/revoked credentials, idempotent retries,
artifact authorization, durable worker lease recovery, and model prompt
injection without commit capability.

`SqliteProcessCrashTest` launches a separate JVM and terminates it at every
documented commit stage. Reopening the database must show only genesis: no
operation, candidate revision, checkpoint, projection change, audit row, or
head movement can survive a pre-commit process crash.

Authenticated `/v1` traffic is limited per identity in fixed UTC minute
windows. The default is 600 requests per minute and can be set with
`STORYBLOCK_SECURITY_RATE_LIMIT_PER_MINUTE`. Rejection uses the stable 429
problem contract and a `Retry-After` header.

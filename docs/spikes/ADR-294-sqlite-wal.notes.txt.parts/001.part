# ADR-294 SQLite WAL and JDBC Concurrency Spike

- Date: 2026-08-21
- Java: 21
- SQLite JDBC: Xerial 3.53.2.1
- Pool: HikariCP, maximum four physical connections
- Database: local filesystem, WAL, `synchronous=FULL`

## Acceptance Evidence

Every physical connection is created through `VerifyingSqliteDataSource`. It
reads and rejects mismatches in `journal_mode`, `synchronous`, `foreign_keys`,
`busy_timeout`, Xerial explicit read-only mode, and load-extension state before
Hikari can lend the connection.

Read work sets both JDBC read-only state and `PRAGMA query_only=true`. Write work
upgrades the empty deferred transaction to `BEGIN IMMEDIATE` before application
SQL. This explicit setup is required because Xerial's `Statement.executeUpdate`
native path does not call its transaction-mode enforcement hook. Pool cleanup
clears both read-only state and `query_only`.

The integration suite covers:

- four simultaneously borrowed and independently verified physical connections;
- write rejection in an explicit read-only transaction and repeated pool reuse;
- a held writer plus a second 25 ms timeout pool, producing
  `sqlite_busy_total=1`;
- two independent writer JVMs and two independent reader JVMs sharing one WAL
  database, followed by an exact row-count and passive-checkpoint assertion.

Run the acceptance suite with:

```bash
./mvnw -o -pl modules/storage-sqlite -am test
```

## Measured Run

The operator harness was run with two writer processes, two reader processes,
50 writes per writer, and 200 reads per reader:

```json
{"writer_processes":2,"reader_processes":2,"writes":100,"reads":400,"final_rows":100,"busy_total":0,"connection_verifications":6,"writer_wait_ms":2,"max_transaction_ms":22,"max_observed_rows":51,"checkpoint":{"busy":0,"log_frames":0,"checkpointed_frames":0,"duration_ms":0},"elapsed_ms":1768}
```

Normal short commits completed without a busy timeout; the controlled lock test
proves busy failures are counted when they occur. Closing the worker processes
allowed SQLite to checkpoint before the final explicit passive checkpoint, so
that final report contained zero remaining WAL frames.

## Conclusion

The proposed single-writer topology supports concurrent readers and short
commits under this Phase 0 workload, with observable contention and checkpoint
state. This is architecture-spike evidence, not the later sustained 2 commits/s
SLO qualification. Run the standalone harness only against a disposable
database path because it owns the `storyblock_spike_commits` table.

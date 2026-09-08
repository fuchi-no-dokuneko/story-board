package dev.storyblock.storage.sqlite;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.Report;

final class WalProcessReport {
  static Report collect(Path absoluteDatabase, int writerProcesses, int readerProcesses, int writesPerProcess, List<Properties> results, long started) throws Exception {
      long writes = SqliteWalSpikeSum.sum(results, "writes");
      long reads = SqliteWalSpikeSum.sum(results, "reads");
      long busy = SqliteWalSpikeSum.sum(results, "busy_total");
      long connectionVerifications = SqliteWalSpikeSum.sum(results, "connection_verifications");
      long writerWaitMillis = SqliteWalSpikeSum.sum(results, "writer_wait_ms");
      long maxTransactionMillis = SqliteWalSpikeMax.max(results, "max_transaction_ms");
      long maxObservedRows = SqliteWalSpikeMax.max(results, "max_observed_rows");
      long expectedRows = (long) writerProcesses * writesPerProcess;

      try (SqliteDatabase database = SqliteDatabase.open(absoluteDatabase)) {
        long finalRows = SqliteWalSpikeCountRows.countRows(database);
        SqliteWalCheckpoint checkpoint = database.checkpointPassive();
        if (writes != expectedRows || finalRows != expectedRows) {
          throw new IllegalStateException(
              "WAL spike lost writes: expected=" + expectedRows
                  + ", worker-reported=" + writes
                  + ", stored=" + finalRows
          );
        }
        return new Report(
            writerProcesses,
            readerProcesses,
            writes,
            reads,
            finalRows,
            busy,
            connectionVerifications,
            writerWaitMillis,
            maxTransactionMillis,
            maxObservedRows,
            checkpoint,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        );
      }  }
}

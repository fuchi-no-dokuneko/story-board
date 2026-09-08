package dev.storyblock.storage.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.WorkerProcess;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.Report;

final class SqliteWalSpikeRunMultiProcessFactory {
  static Report runMultiProcess(Path databasePath, int writerProcesses, int readerProcesses, int writesPerProcess, int readsPerProcess) throws Exception {
    SqliteWalSpikeRequirePositive.requirePositive(writerProcesses, "writerProcesses");
    SqliteWalSpikeRequirePositive.requirePositive(readerProcesses, "readerProcesses");
    SqliteWalSpikeRequirePositive.requirePositive(writesPerProcess, "writesPerProcess");
    SqliteWalSpikeRequirePositive.requirePositive(readsPerProcess, "readsPerProcess");
    Path absoluteDatabase = databasePath.toAbsolutePath().normalize();
    SqliteWalSpikeInitialize.initialize(absoluteDatabase);

    Path workerDirectory = Files.createTempDirectory("storyblock-wal-workers-");
    List<WorkerProcess> workers = new ArrayList<>();
    long synchronizedStart = System.currentTimeMillis() + 1_000L;
    long started = System.nanoTime();
    try {
      for (int index = 0; index < writerProcesses; index++) {
        workers.add(SqliteWalSpikeStartWorker.startWorker(
            "writer-" + index,
            "writer",
            absoluteDatabase,
            writesPerProcess,
            synchronizedStart,
            workerDirectory
        ));
      }
      for (int index = 0; index < readerProcesses; index++) {
        workers.add(SqliteWalSpikeStartWorker.startWorker(
            "reader-" + index,
            "reader",
            absoluteDatabase,
            readsPerProcess,
            synchronizedStart,
            workerDirectory
        ));
      }

      List<Properties> results = new ArrayList<>();
      for (WorkerProcess worker : workers) {
        SqliteWalSpikeAwaitWorker.awaitWorker(worker);
        results.add(SqliteWalSpikeLoad.load(worker.resultFile()));
      }

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
      }
    } finally {
      SqliteWalSpikeStopRemainingWorkers.stopRemainingWorkers(workers);
      SqliteWalSpikeCleanupWorkerFiles.cleanupWorkerFiles(workers, workerDirectory);
    }
  }
}

package dev.storyblock.storage.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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

      return WalProcessReport.collect(absoluteDatabase, writerProcesses, readerProcesses, writesPerProcess, results, started);
    } finally {
      SqliteWalSpikeStopRemainingWorkers.stopRemainingWorkers(workers);
      SqliteWalSpikeCleanupWorkerFiles.cleanupWorkerFiles(workers, workerDirectory);
    }
  }
}

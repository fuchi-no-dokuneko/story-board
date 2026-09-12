package dev.storyblock.storage.sqlite;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class SqliteWalSpikeRunWorker {
    static void runWorker(String[] arguments) throws Exception {
        if (arguments.length != 7) {
            throw new IllegalArgumentException("Invalid WAL spike worker arguments");
        }
        String role = arguments[1];
        String workerId = arguments[2];
        Path databasePath = Path.of(arguments[3]);
        int operations = Integer.parseInt(arguments[4]);
        long startAtMillis = Long.parseLong(arguments[5]);
        Path resultFile = Path.of(arguments[6]);
        SqliteWalSpikeSleepUntil.sleepUntil(startAtMillis);

        long completedWrites = 0;
        long completedReads = 0;
        long maxObservedRows = 0;
        try (SqliteDatabase database = SqliteDatabase.open(databasePath)) {
            if (role.equals("writer")) {
                completedWrites = SqliteWalSpikeRunWriter.runWriter(database, workerId, operations);
            } else if (role.equals("reader")) {
                maxObservedRows = SqliteWalSpikeRunReader.runReader(database, operations);
                completedReads = operations;
            } else {
                throw new IllegalArgumentException("Unknown worker role " + role);
            }

            SqliteMetrics.Snapshot metrics = database.metrics();
            Properties result = new Properties();
            result.setProperty("role", role);
            result.setProperty("writes", Long.toString(completedWrites));
            result.setProperty("reads", Long.toString(completedReads));
            result.setProperty("max_observed_rows", Long.toString(maxObservedRows));
            result.setProperty("busy_total", Long.toString(metrics.sqliteBusyTotal()));
            result.setProperty(
                    "connection_verifications",
                    Long.toString(metrics.connectionVerifications())
            );
            result.setProperty("writer_wait_ms", Long.toString(metrics.writerWaitMillis()));
            result.setProperty(
                    "max_transaction_ms",
                    Long.toString(metrics.maxTransactionMillis())
            );
            try (OutputStream output = Files.newOutputStream(resultFile)) {
                result.store(output, "StoryBlock SQLite WAL spike worker result");
            }
        }
    }
}

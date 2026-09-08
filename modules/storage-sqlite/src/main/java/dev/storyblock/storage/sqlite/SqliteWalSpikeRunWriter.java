package dev.storyblock.storage.sqlite;

import java.sql.SQLException;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.PROCESS_TIMEOUT;

final class SqliteWalSpikeRunWriter {
    static long runWriter(
            SqliteDatabase database,
            String workerId,
            int writes
    ) throws Exception {
        long completed = 0;
        long deadline = System.nanoTime() + PROCESS_TIMEOUT.toNanos();
        while (completed < writes) {
            try {
                database.write(connection -> {
                    try (var statement = connection.prepareStatement("""
                            INSERT INTO storyblock_spike_commits(worker_id, committed_at_ms)
                            VALUES (?, ?)
                            """)) {
                        statement.setString(1, workerId);
                        statement.setLong(2, System.currentTimeMillis());
                        statement.executeUpdate();
                    }
                    return null;
                });
                completed++;
            } catch (SQLException exception) {
                if (!SqliteMetrics.isBusy(exception) || System.nanoTime() >= deadline) {
                    throw exception;
                }
                Thread.sleep(2L);
            }
        }
        return completed;
    }
}

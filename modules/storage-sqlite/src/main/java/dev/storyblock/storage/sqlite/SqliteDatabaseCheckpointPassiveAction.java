package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

final class SqliteDatabaseCheckpointPassiveAction {
    static SqliteWalCheckpoint checkpointPassive(SqliteDatabase self) throws SQLException {
        long started = System.nanoTime();
        try (Connection connection = self.pool.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA wal_checkpoint(PASSIVE)")) {
            if (!result.next()) {
                throw new SQLException("wal_checkpoint returned no result");
            }
            SqliteWalCheckpoint checkpoint = new SqliteWalCheckpoint(
                    result.getInt(1),
                    result.getInt(2),
                    result.getInt(3),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            );
            self.metrics.recordCheckpoint(checkpoint.durationMillis());
            return checkpoint;
        } catch (SQLException exception) {
            self.metrics.recordFailure(exception);
            throw exception;
        }
    }
}

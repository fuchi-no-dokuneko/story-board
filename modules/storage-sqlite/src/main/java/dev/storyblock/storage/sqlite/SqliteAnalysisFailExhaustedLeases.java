package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteAnalysisFailExhaustedLeases {
    static void failExhaustedLeases(Connection connection, Instant now)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'failed', lease_owner = NULL, lease_until = NULL,
                    failure_code = 'attempts_exhausted', updated_at = ?
                WHERE status = 'running'
                  AND julianday(lease_until) <= julianday(?)
                  AND attempt >= max_attempts
                """)) {
            statement.setString(1, now.toString());
            statement.setString(2, now.toString());
            statement.executeUpdate();
        }
    }
}

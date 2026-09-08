package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import dev.storyblock.style.StyleAnalysisClaimCommand;
import dev.storyblock.style.StyleAnalysisJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisFenceLease {
    static void update(Connection connection, StyleAnalysisJob current, StyleAnalysisClaimCommand command, Instant leaseUntil, int attempt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'running', lease_owner = ?, lease_until = ?, attempt = ?,
                    updated_at = ?, failure_code = NULL
                WHERE job_id = ?
                  AND attempt = ?
                  AND (
                      status = 'queued'
                      OR (status = 'running' AND julianday(lease_until) <= julianday(?))
                  )
                """)) {
            statement.setString(1, command.leaseOwner());
            statement.setString(2, leaseUntil.toString());
            statement.setInt(3, attempt);
            statement.setString(4, command.claimedAt().toString());
            statement.setString(5, current.jobId().value());
            statement.setInt(6, current.attempt());
            statement.setString(7, command.claimedAt().toString());
            if (statement.executeUpdate() != 1) {
                throw new StorageException("Style analysis claim lost its fencing race");
            }
        }

    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.style.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisCompleteLease {
    static void complete(Connection connection, StyleAnalysisCompletionCommand command) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'succeeded', lease_owner = NULL, lease_until = NULL,
                    result_artifact_id = ?, result_hash = ?, updated_at = ?
                WHERE job_id = ? AND status = 'running'
                  AND lease_owner = ? AND attempt = ?
                """)) {
            statement.setString(1, command.trace().artifactId().value());
            statement.setString(2, command.resultHash());
            statement.setString(3, command.completedAt().toString());
            statement.setString(4, command.jobId().value());
            statement.setString(5, command.leaseOwner());
            statement.setInt(6, command.attempt());
            if (statement.executeUpdate() != 1) {
                throw new StyleAnalysisLeaseConflictException(
                        "Style analysis lease changed before result commit",
                        SqliteAnalysisGetJob.getJob(connection, command.jobId()).statusHash()
                );
            }
        }

    }
}

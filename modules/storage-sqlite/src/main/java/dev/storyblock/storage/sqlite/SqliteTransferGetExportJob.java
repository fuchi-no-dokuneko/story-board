package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.MissingExportJobException;
import dev.storyblock.storage.StoredExportJob;
import java.sql.*;

final class SqliteTransferGetExportJob {
    static StoredExportJob getExportJob(Connection connection, Ids.JobId jobId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT job_id, novel_id, revision_id, revision_sequence, revision_hash,
                       format, result_artifact_id, created_at
                FROM export_jobs
                WHERE job_id = ?
                """)) {
            statement.setString(1, jobId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingExportJobException(jobId);
                }
                return SqliteTransferReadExportJob.readExportJob(result);
            }
        }
    }
}

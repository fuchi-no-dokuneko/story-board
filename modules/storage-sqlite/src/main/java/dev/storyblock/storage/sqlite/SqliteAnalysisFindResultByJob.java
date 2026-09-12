package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisFindResultByJob {
    static Optional<StoredResult> findResultByJob(
            Connection connection,
            Ids.JobId jobId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT analysis_id, job_id, summary_json, result_artifact_id,
                       trace_content_hash, trace_uncompressed_bytes, trace_expires_at,
                       result_hash, completed_at, submission_idempotency_key,
                       submission_request_hash
                FROM analysis_runs
                WHERE job_id = ?
                """)) {
            statement.setString(1, jobId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StoredResult(
                        SqliteAnalysisReadResult.readResult(result),
                        result.getString("submission_idempotency_key"),
                        result.getString("submission_request_hash")
                ));
            }
        }
    }
}

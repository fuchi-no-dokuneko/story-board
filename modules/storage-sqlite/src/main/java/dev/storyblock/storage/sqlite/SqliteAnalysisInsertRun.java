package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteAnalysisInsertRun {
    static void insertRun(
            Connection connection,
            StyleAnalysisJob job,
            StyleAnalysisCompletionCommand command
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_runs(
                    analysis_id, job_id, summary_json, result_hash,
                    result_artifact_id, trace_content_hash, trace_uncompressed_bytes,
                    trace_expires_at, submission_idempotency_key,
                    submission_request_hash, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, job.analysisId().value());
            statement.setString(2, job.jobId().value());
            statement.setString(3, CanonicalJson.string(command.summary().canonicalValue()));
            statement.setString(4, command.resultHash());
            statement.setString(5, command.trace().artifactId().value());
            statement.setString(6, command.trace().contentHash());
            statement.setInt(7, command.trace().uncompressedBytes());
            statement.setString(8, command.trace().expiresAt().toString());
            statement.setString(9, command.idempotencyKey());
            statement.setString(10, command.requestHash());
            statement.setString(11, command.completedAt().toString());
            statement.executeUpdate();
        }
    }
}

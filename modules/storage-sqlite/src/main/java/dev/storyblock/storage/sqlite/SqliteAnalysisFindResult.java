package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisResult;
import java.sql.*;
import java.util.Optional;

final class SqliteAnalysisFindResult {
    static Optional<StyleAnalysisResult> findResult(
            Connection connection,
            Ids.StyleAnalysisId analysisId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT analysis_id, job_id, summary_json, result_artifact_id,
                       trace_content_hash, trace_uncompressed_bytes, trace_expires_at,
                       result_hash, completed_at
                FROM analysis_runs
                WHERE analysis_id = ?
                """)) {
            statement.setString(1, analysisId.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(SqliteAnalysisReadResult.readResult(result)) : Optional.empty();
            }
        }
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisResult;
import dev.storyblock.style.StyleAnalysisSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteAnalysisReadResult {
    static StyleAnalysisResult readResult(ResultSet result) throws SQLException {
        return new StyleAnalysisResult(
                new Ids.StyleAnalysisId(result.getString("analysis_id")),
                new Ids.JobId(result.getString("job_id")),
                StyleAnalysisSummary.fromCanonical(SqliteAnalysisParseObject.parseObject(
                        result.getString("summary_json"), "style analysis summary"
                )),
                new Ids.ArtifactId(result.getString("result_artifact_id")),
                result.getString("trace_content_hash"),
                result.getInt("trace_uncompressed_bytes"),
                Instant.parse(result.getString("trace_expires_at")),
                result.getString("result_hash"),
                Instant.parse(result.getString("completed_at"))
        );
    }
}

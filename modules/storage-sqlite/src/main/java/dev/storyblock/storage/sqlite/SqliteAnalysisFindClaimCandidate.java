package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisJob;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisFindClaimCandidate {
    static Optional<StyleAnalysisJob> findClaimCandidate(
            Connection connection,
            Ids.NovelId novelId,
            Instant now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs " + """
                        WHERE novel_id = ? AND attempt < max_attempts
                          AND (
                              status = 'queued'
                              OR (status = 'running'
                                  AND julianday(lease_until) <= julianday(?))
                          )
                        ORDER BY created_at, job_id
                        LIMIT 1
                        """
        )) {
            statement.setString(1, novelId.value());
            statement.setString(2, now.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(SqliteAnalysisReadJob.readJob(result)) : Optional.empty();
            }
        }
    }
}

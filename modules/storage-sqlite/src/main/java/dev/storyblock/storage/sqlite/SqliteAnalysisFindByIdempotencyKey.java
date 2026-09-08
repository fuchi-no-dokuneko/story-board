package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisJob;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisFindByIdempotencyKey {
    static Optional<StyleAnalysisJob> findByIdempotencyKey(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs "
                        + "WHERE novel_id = ? AND idempotency_key = ?"
        )) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(SqliteAnalysisReadJob.readJob(result)) : Optional.empty();
            }
        }
    }
}

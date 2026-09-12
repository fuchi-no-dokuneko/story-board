package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.MissingStyleAnalysisException;
import dev.storyblock.style.StyleAnalysisJob;
import java.sql.*;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisGetAnalysis {
    static StyleAnalysisJob getAnalysis(
            Connection connection,
            Ids.StyleAnalysisId analysisId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs WHERE analysis_id = ?"
        )) {
            statement.setString(1, analysisId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleAnalysisException(analysisId);
                }
                return SqliteAnalysisReadJob.readJob(result);
            }
        }
    }
}

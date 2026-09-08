package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.MissingStyleAnalysisJobException;
import dev.storyblock.style.StyleAnalysisJob;
import java.sql.*;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisGetJob {
    static StyleAnalysisJob getJob(Connection connection, Ids.JobId jobId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs WHERE job_id = ?"
        )) {
            statement.setString(1, jobId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleAnalysisJobException(jobId);
                }
                return SqliteAnalysisReadJob.readJob(result);
            }
        }
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobSaveResult;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

final class SqliteAnalysisCreateJob {
    static StyleAnalysisJobSaveResult createJob(
            Connection connection,
            StyleAnalysisJob job
    ) throws SQLException {
        Optional<StyleAnalysisJob> prior = SqliteAnalysisFindByIdempotencyKey.findByIdempotencyKey(
                connection, job.snapshot().novelId(), job.idempotencyKey()
        );
        if (prior.isPresent()) {
            StyleAnalysisJob stored = prior.get();
            if (!stored.requestHash().equals(job.requestHash())) {
                throw new IdempotencyConflictException(
                        job.idempotencyKey(), stored.requestHash(), job.requestHash()
                );
            }
            return new StyleAnalysisJobSaveResult(stored, true);
        }
        SqliteAnalysisRequireSnapshotVersions.requireSnapshotVersions(connection, job.snapshot());
        SqliteAnalysisInsertJob.insert(connection, job);
        return new StyleAnalysisJobSaveResult(job, false);
    }
}

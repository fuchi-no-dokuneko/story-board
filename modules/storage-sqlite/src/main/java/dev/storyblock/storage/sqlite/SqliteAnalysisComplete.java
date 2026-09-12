package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisComplete {
    static StyleAnalysisCompletionResult complete(
            Connection connection,
            StyleAnalysisCompletionCommand command
    ) throws SQLException {
        Optional<StoredResult> prior = SqliteAnalysisFindResultByJob.findResultByJob(connection, command.jobId());
        if (prior.isPresent()) {
            StoredResult stored = prior.get();
            if (!stored.result().resultHash().equals(command.resultHash())) {
                throw new StyleAnalysisResultConflictException(
                        stored.result().resultHash(), command.resultHash()
                );
            }
            if (stored.idempotencyKey().equals(command.idempotencyKey())
                    && !stored.requestHash().equals(command.requestHash())) {
                throw new IdempotencyConflictException(
                        command.idempotencyKey(),
                        stored.requestHash(),
                        command.requestHash()
                );
            }
            return new StyleAnalysisCompletionResult(
                    SqliteAnalysisGetJob.getJob(connection, command.jobId()), stored.result(), true
            );
        }

        StyleAnalysisJob job = SqliteAnalysisGetJob.getJob(connection, command.jobId());
        SqliteAnalysisRequireActiveLease.requireActiveLease(job, command);
        SqliteAnalysisRequireCompletionVersions.requireCompletionVersions(job, command);
        SqliteAnalysisInsertTraceArtifact.insertTraceArtifact(connection, job, command.trace());
        SqliteAnalysisInsertRun.insertRun(connection, job, command);
        SqliteAnalysisInsertWindows.insertWindows(connection, job.analysisId(), command.windows());
        SqliteAnalysisInsertAnalysisArtifact.insertAnalysisArtifact(connection, command.trace());
        SqliteAnalysisCompleteLease.complete(connection, command);
        StyleAnalysisJob completed = SqliteAnalysisGetJob.getJob(connection, command.jobId());
        StyleAnalysisResult result = SqliteAnalysisFindResultByJob.findResultByJob(connection, command.jobId())
                .orElseThrow(() -> new StorageException(
                        "Committed style analysis result cannot be read"
                )).result();
        return new StyleAnalysisCompletionResult(completed, result, false);
    }
}

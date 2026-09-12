package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteTransferData.*;

final class SqliteTransferCreateCompletedExport {
    static ExportJobResult createCompletedExport(
            Connection connection,
            ExportJobRequest request
    ) throws SQLException {
        Optional<StoredExportWithHash> prior = SqliteTransferFindExportByIdempotencyKey.findExportByIdempotencyKey(
                connection, request.novelId(), request.idempotencyKey()
        );
        if (prior.isPresent()) {
            StoredExportWithHash stored = prior.get();
            if (!stored.requestHash().equals(request.requestHash())) {
                throw new IdempotencyConflictException(
                        request.idempotencyKey(), stored.requestHash(), request.requestHash()
                );
            }
            return new ExportJobResult(stored.job(), true);
        }

        RevisionRef actualHead = SqliteTransferRequireHead.requireHead(connection, request.novelId());
        if (!actualHead.equals(request.expectedHead())) {
            throw new StaleHeadException(request.expectedHead(), actualHead);
        }
        SqliteTransferInsertArtifact.insertArtifact(connection, request.artifact());
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO export_jobs(
                    job_id, novel_id, revision_id, revision_sequence, revision_hash,
                    format, idempotency_key, request_hash, result_artifact_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, request.jobId().value());
            statement.setString(2, request.novelId().value());
            statement.setString(3, request.expectedHead().revisionId().value());
            statement.setLong(4, request.expectedHead().sequence());
            statement.setString(5, request.expectedHead().contentHash());
            statement.setString(6, request.format().canonicalName());
            statement.setString(7, request.idempotencyKey());
            statement.setString(8, request.requestHash());
            statement.setString(9, request.artifact().artifactId().value());
            statement.setString(10, request.createdAt().toString());
            statement.executeUpdate();
        }
        return new ExportJobResult(SqliteTransferToStoredJob.toStoredJob(request), false);
    }
}

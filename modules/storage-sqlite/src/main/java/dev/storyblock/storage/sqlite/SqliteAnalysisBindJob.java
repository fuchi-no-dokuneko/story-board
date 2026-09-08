package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisSnapshot;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteAnalysisBindJob {
    static void bind(PreparedStatement statement, StyleAnalysisJob job) throws SQLException {
        StyleAnalysisSnapshot snapshot = job.snapshot();
        statement.setString(1, job.jobId().value());
        statement.setString(2, job.analysisId().value());
        statement.setString(3, snapshot.novelId().value());
        statement.setString(4, snapshot.revisionId().value());
        statement.setString(5, snapshot.revisionHash());
        statement.setString(6, snapshot.profileVersion().profileId().value());
        statement.setString(7, snapshot.profileVersion().versionId().value());
        statement.setString(8, snapshot.profileVersionHash());
        statement.setString(9, snapshot.analyzerContractHash());
        statement.setString(10, snapshot.windowConfigurationHash());
        statement.setString(11, snapshot.snapshotHash());
        statement.setString(12, CanonicalJson.string(snapshot.canonicalValue()));
        statement.setString(13, job.status().canonicalName());
        statement.setInt(14, job.maxAttempts());
        statement.setString(15, job.idempotencyKey());
        statement.setString(16, job.requestHash());
        statement.setString(17, job.auditContext().requestId());
        statement.setString(18, job.auditContext().actorId());
        SqliteAnalysisNullableString.nullableString(
                statement,
                19,
                job.auditContext().actorKeyId() == null
                        ? null : job.auditContext().actorKeyId().value()
        );
        statement.setString(20, job.retentionUntil().toString());
        statement.setString(21, job.createdAt().toString());
        statement.setString(22, job.updatedAt().toString());

    }
}

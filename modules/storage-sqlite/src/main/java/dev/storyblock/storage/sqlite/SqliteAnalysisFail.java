package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobStatus;
import dev.storyblock.style.StyleAnalysisLeaseConflictException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

final class SqliteAnalysisFail {
    static StyleAnalysisJob fail(
            Connection connection,
            Ids.JobId jobId,
            String leaseOwner,
            int attempt,
            String expectedStatusHash,
            String failureCode,
            Instant failedAt
    ) throws SQLException {
        StyleAnalysisJob job = SqliteAnalysisGetJob.getJob(connection, jobId);
        if (job.status() != StyleAnalysisJobStatus.RUNNING
                || !job.statusHash().equals(expectedStatusHash)
                || !Objects.equals(job.leaseOwner(), leaseOwner)
                || job.attempt() != attempt
                || failedAt.isBefore(job.updatedAt())
                || !failedAt.isBefore(job.leaseUntil())) {
            throw new StyleAnalysisLeaseConflictException(
                    "Style analysis failure submission does not own the active lease",
                    job.statusHash()
            );
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'failed', lease_owner = NULL, lease_until = NULL,
                    failure_code = ?, updated_at = ?
                WHERE job_id = ? AND status = 'running'
                  AND lease_owner = ? AND attempt = ?
                """)) {
            statement.setString(1, failureCode);
            statement.setString(2, failedAt.toString());
            statement.setString(3, jobId.value());
            statement.setString(4, leaseOwner);
            statement.setInt(5, attempt);
            if (statement.executeUpdate() != 1) {
                throw new StyleAnalysisLeaseConflictException(
                        "Style analysis lease changed before failure commit",
                        SqliteAnalysisGetJob.getJob(connection, jobId).statusHash()
                );
            }
        }
        return SqliteAnalysisGetJob.getJob(connection, jobId);
    }
}

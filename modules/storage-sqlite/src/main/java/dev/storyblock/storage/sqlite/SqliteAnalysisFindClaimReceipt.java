package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisSnapshot;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisFindClaimReceipt {
    static Optional<ClaimReceipt> findClaimReceipt(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT receipt.request_hash, receipt.job_id, receipt.lease_owner,
                       receipt.attempt, receipt.lease_until,
                       receipt.claimed_status_hash, job.analysis_id, job.snapshot_json,
                       job.retention_until
                FROM analysis_claim_receipts AS receipt
                LEFT JOIN analysis_jobs AS job ON job.job_id = receipt.job_id
                WHERE receipt.novel_id = ? AND receipt.idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String job = result.getString("job_id");
                return Optional.of(new ClaimReceipt(
                        result.getString("request_hash"),
                        job == null ? null : new Ids.JobId(job),
                        job == null ? null : new Ids.StyleAnalysisId(
                                result.getString("analysis_id")
                        ),
                        job == null ? null : StyleAnalysisSnapshot.fromCanonical(
                                SqliteAnalysisParseObject.parseObject(
                                        result.getString("snapshot_json"),
                                        "style claim snapshot"
                                )
                        ),
                        result.getString("lease_owner"),
                        job == null ? 0 : result.getInt("attempt"),
                        SqliteAnalysisNullableInstant.nullableInstant(result.getString("lease_until")),
                        SqliteAnalysisNullableInstant.nullableInstant(result.getString("retention_until")),
                        result.getString("claimed_status_hash")
                ));
            }
        }
    }
}

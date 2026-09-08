package dev.storyblock.storage.sqlite;

import dev.storyblock.style.StyleAnalysisClaimCommand;
import dev.storyblock.style.StyleAnalysisLease;
import java.sql.*;

final class SqliteAnalysisInsertClaimReceipt {
    static void insertClaimReceipt(
            Connection connection,
            StyleAnalysisClaimCommand command,
            StyleAnalysisLease lease
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_claim_receipts(
                    novel_id, idempotency_key, request_hash, job_id, lease_owner,
                    attempt, lease_until, claimed_status_hash, claimed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, command.novelId().value());
            statement.setString(2, command.idempotencyKey());
            statement.setString(3, command.requestHash());
            SqliteAnalysisNullableString.nullableString(statement, 4, lease == null ? null : lease.jobId().value());
            statement.setString(5, command.leaseOwner());
            if (lease == null) {
                statement.setNull(6, Types.INTEGER);
            } else {
                statement.setInt(6, lease.attempt());
            }
            SqliteAnalysisNullableString.nullableString(
                    statement, 7, lease == null ? null : lease.leaseUntil().toString()
            );
            SqliteAnalysisNullableString.nullableString(
                    statement,
                    8,
                    lease == null ? null : lease.claimedStatusHash()
            );
            statement.setString(9, command.claimedAt().toString());
            statement.executeUpdate();
        }
    }
}

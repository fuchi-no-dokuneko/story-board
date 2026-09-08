package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.rewrite.policy.ReserveRewriteCandidateCommand;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteReservationInsertReservation {
    static void insertReservation(
            Connection connection,
            ReserveRewriteCandidateCommand command
    ) throws SQLException {
        RewriteCandidateReservation value = command.reservation();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rewrite_candidate_reservations(
                    proposal_id, novel_id, analysis_id, revision_id, revision_hash,
                    profile_version_id, eligibility_hash, worker_input_hash,
                    reservation_hash, idempotency_key, request_hash,
                    reservation_json, created_at, cooldown_until
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, value.proposalId().value());
            statement.setString(2, value.novelId().value());
            statement.setString(3, value.eligibility().analysisId().value());
            statement.setString(4, value.eligibility().revisionId().value());
            statement.setString(5, value.eligibility().revisionHash());
            statement.setString(6, value.eligibility().profileVersionId().value());
            statement.setString(7, value.eligibility().eligibilityHash());
            statement.setString(8, value.workerInput().inputHash());
            statement.setString(9, value.reservationHash());
            statement.setString(10, command.idempotencyKey());
            statement.setString(11, command.requestHash());
            statement.setString(12, CanonicalJson.string(value.canonicalValue()));
            statement.setString(13, value.createdAt().toString());
            statement.setString(14, value.cooldownUntil().toString());
            statement.executeUpdate();
        }
    }
}

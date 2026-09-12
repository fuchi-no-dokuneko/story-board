package dev.storyblock.storage.sqlite;

import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteReservationInsertFindings {
    static void insertFindings(
            Connection connection,
            RewriteCandidateReservation value
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rewrite_reserved_findings(
                    analysis_id, finding_id, proposal_id
                ) VALUES (?, ?, ?)
                """)) {
            for (String findingId : value.eligibility().findingIds()) {
                statement.setString(1, value.eligibility().analysisId().value());
                statement.setString(2, findingId);
                statement.setString(3, value.proposalId().value());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}

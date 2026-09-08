package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteFindingAlreadyReservedException;
import java.sql.*;

final class SqliteReservationRequireUnreservedFindings {
    static void requireUnreservedFindings(
            Connection connection,
            RewriteCandidateReservation reservation
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT proposal_id
                FROM rewrite_reserved_findings
                WHERE analysis_id = ? AND finding_id = ?
                """)) {
            for (String findingId : reservation.eligibility().findingIds()) {
                statement.setString(1, reservation.eligibility().analysisId().value());
                statement.setString(2, findingId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        throw new RewriteFindingAlreadyReservedException(
                                findingId, new Ids.ProposalId(result.getString(1))
                        );
                    }
                }
            }
        }
    }
}

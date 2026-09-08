package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.MissingRewriteReservationException;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import java.sql.*;

final class SqliteReservationGet {
    static RewriteCandidateReservation get(
            Connection connection,
            Ids.ProposalId proposalId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT reservation_json
                FROM rewrite_candidate_reservations
                WHERE proposal_id = ?
                """)) {
            statement.setString(1, proposalId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingRewriteReservationException(proposalId);
                }
                return SqliteReservationParse.parse(result.getString(1));
            }
        }
    }
}

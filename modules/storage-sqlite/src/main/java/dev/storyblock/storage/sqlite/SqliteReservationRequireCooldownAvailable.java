package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCooldownException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class SqliteReservationRequireCooldownAvailable {
    static void requireCooldownAvailable(
            Connection connection,
            RewriteCandidateReservation reservation
    ) throws SQLException {
        String placeholders = String.join(",", java.util.Collections.nCopies(
                reservation.eligibility().affectedBlockIds().size(), "?"
        ));
        String sql = """
                SELECT DISTINCT blocks.block_id, reservations.cooldown_until
                FROM rewrite_reserved_blocks blocks
                JOIN rewrite_candidate_reservations reservations
                  ON reservations.proposal_id = blocks.proposal_id
                WHERE blocks.novel_id = ?
                  AND julianday(reservations.cooldown_until) > julianday(?)
                  AND blocks.block_id IN (%s)
                ORDER BY blocks.block_id
                """.formatted(placeholders);
        List<Ids.BlockId> blocked = new ArrayList<>();
        java.time.Instant latest = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reservation.novelId().value());
            statement.setString(2, reservation.createdAt().toString());
            int parameter = 3;
            for (Ids.BlockId blockId : reservation.eligibility().affectedBlockIds()) {
                statement.setString(parameter++, blockId.value());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    blocked.add(new Ids.BlockId(result.getString("block_id")));
                    java.time.Instant until = java.time.Instant.parse(
                            result.getString("cooldown_until")
                    );
                    if (latest == null || until.isAfter(latest)) {
                        latest = until;
                    }
                }
            }
        }
        if (!blocked.isEmpty()) {
            throw new RewriteCooldownException(blocked, latest);
        }
    }
}

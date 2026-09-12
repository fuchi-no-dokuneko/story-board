package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteReservationInsertBlocks {
    static void insertBlocks(
            Connection connection,
            RewriteCandidateReservation value
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rewrite_reserved_blocks(proposal_id, novel_id, block_id)
                VALUES (?, ?, ?)
                """)) {
            for (Ids.BlockId blockId : value.eligibility().affectedBlockIds()) {
                statement.setString(1, value.proposalId().value());
                statement.setString(2, value.novelId().value());
                statement.setString(3, blockId.value());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}

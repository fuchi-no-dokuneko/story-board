package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.StoredMonitorRun;
import java.sql.*;
import java.util.Optional;

final class SqliteMonitorFindByIdempotencyKey {
    static Optional<StoredMonitorRun> findByIdempotencyKey(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT run_id, output_id, output_kind, novel_id, revision_id,
                       revision_hash, target_block_id, neighbor_count, monitor_version,
                       rule_version, affected_blocks_json, idempotency_key, request_hash,
                       request_id, actor_id, actor_key_id, submitted_at
                FROM monitor_runs
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(SqliteMonitorRead.read(connection, result))
                        : Optional.empty();
            }
        }
    }
}

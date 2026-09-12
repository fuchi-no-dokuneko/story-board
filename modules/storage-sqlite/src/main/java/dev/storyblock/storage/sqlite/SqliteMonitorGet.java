package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MissingMonitorRunException;
import dev.storyblock.monitor.StoredMonitorRun;
import java.sql.*;

final class SqliteMonitorGet {
    static StoredMonitorRun get(
            Connection connection,
            Ids.NovelId novelId,
            Ids.MonitorRunId runId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT run_id, output_id, output_kind, novel_id, revision_id,
                       revision_hash, target_block_id, neighbor_count, monitor_version,
                       rule_version, affected_blocks_json, idempotency_key, request_hash,
                       request_id, actor_id, actor_key_id, submitted_at
                FROM monitor_runs
                WHERE novel_id = ? AND run_id = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, runId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingMonitorRunException(novelId, runId);
                }
                return SqliteMonitorRead.read(connection, result);
            }
        }
    }
}

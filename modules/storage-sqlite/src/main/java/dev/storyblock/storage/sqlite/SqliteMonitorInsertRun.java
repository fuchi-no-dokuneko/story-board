package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.monitor.MonitorBlockFingerprint;
import dev.storyblock.monitor.StoredMonitorRun;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteMonitorInsertRun {
    static void insert(Connection connection, StoredMonitorRun run) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO monitor_runs(
                    run_id, output_id, output_kind, novel_id, revision_id,
                    revision_hash, target_block_id, neighbor_count, monitor_version,
                    rule_version, affected_blocks_json, idempotency_key, request_hash,
                    request_id, actor_id, actor_key_id, submitted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, run.runId().value());
            statement.setString(2, run.outputId().value());
            statement.setString(3, run.output().kind().canonicalName());
            statement.setString(4, run.novelId().value());
            statement.setString(5, run.revisionId().value());
            statement.setString(6, run.revisionHash());
            statement.setString(7, run.targetBlockId().value());
            statement.setInt(8, run.neighborCount());
            statement.setString(9, run.monitorVersion());
            statement.setString(10, run.ruleVersion());
            statement.setString(11, CanonicalJson.string(run.affectedBlocks().stream()
                    .map(MonitorBlockFingerprint::canonicalValue).toList()));
            statement.setString(12, run.idempotencyKey());
            statement.setString(13, run.requestHash());
            statement.setString(14, run.auditContext().requestId());
            statement.setString(15, run.auditContext().actorId());
            if (run.auditContext().actorKeyId() == null) {
                statement.setNull(16, java.sql.Types.VARCHAR);
            } else {
                statement.setString(16, run.auditContext().actorKeyId().value());
            }
            statement.setString(17, run.submittedAt().toString());
            statement.executeUpdate();
        }

    }
}

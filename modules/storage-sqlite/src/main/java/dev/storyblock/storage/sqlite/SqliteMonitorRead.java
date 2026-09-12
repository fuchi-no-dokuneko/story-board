package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorOutput;
import dev.storyblock.monitor.MonitorOutputKind;
import dev.storyblock.monitor.StoredMonitorRun;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.StorageException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteMonitorRead {
    static StoredMonitorRun read(Connection connection, ResultSet result)
            throws SQLException {
        Ids.MonitorRunId runId = new Ids.MonitorRunId(result.getString("run_id"));
        Ids.MonitorOutputId outputId = Ids.MonitorOutputId.parse(
                result.getString("output_id")
        );
        MonitorOutputKind kind = MonitorOutputKind.fromCanonicalName(
                result.getString("output_kind")
        );
        MonitorOutput output = SqliteMonitorReadOutput.readOutput(connection, runId, outputId, kind);
        if (output.kind() != kind) {
            throw new StorageException("Stored monitor output kind does not match its run");
        }
        String actorKey = result.getString("actor_key_id");
        Instant submittedAt = Instant.parse(result.getString("submitted_at"));
        return new StoredMonitorRun(
                runId,
                outputId,
                new Ids.NovelId(result.getString("novel_id")),
                new Ids.RevisionId(result.getString("revision_id")),
                result.getString("revision_hash"),
                new Ids.BlockId(result.getString("target_block_id")),
                result.getInt("neighbor_count"),
                result.getString("monitor_version"),
                result.getString("rule_version"),
                SqliteMonitorParseFingerprints.parseFingerprints(result.getString("affected_blocks_json")),
                output,
                result.getString("idempotency_key"),
                result.getString("request_hash"),
                new AuditContext(
                        result.getString("request_id"),
                        result.getString("actor_id"),
                        actorKey == null ? null : new Ids.AccessKeyId(actorKey),
                        submittedAt
                ),
                submittedAt
        );
    }
}

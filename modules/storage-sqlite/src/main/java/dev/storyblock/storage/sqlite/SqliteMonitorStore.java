package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MissingMonitorRunException;
import dev.storyblock.monitor.MonitorBlockFingerprint;
import dev.storyblock.monitor.MonitorOutput;
import dev.storyblock.monitor.MonitorOutputKind;
import dev.storyblock.monitor.MonitorSaveResult;
import dev.storyblock.monitor.StoredMonitorRun;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.StorageException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class SqliteMonitorStore {
    private SqliteMonitorStore() {
    }

    static MonitorSaveResult save(Connection connection, StoredMonitorRun run)
            throws SQLException {
        Optional<StoredMonitorRun> prior = findByIdempotencyKey(
                connection, run.novelId(), run.idempotencyKey()
        );
        if (prior.isPresent()) {
            StoredMonitorRun stored = prior.get();
            if (!stored.requestHash().equals(run.requestHash())) {
                throw new IdempotencyConflictException(
                        run.idempotencyKey(), stored.requestHash(), run.requestHash()
                );
            }
            return new MonitorSaveResult(stored, true);
        }

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
        insertOutput(connection, run);
        return new MonitorSaveResult(run, false);
    }

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
                return read(connection, result);
            }
        }
    }

    private static Optional<StoredMonitorRun> findByIdempotencyKey(
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
                        ? Optional.of(read(connection, result))
                        : Optional.empty();
            }
        }
    }

    private static StoredMonitorRun read(Connection connection, ResultSet result)
            throws SQLException {
        Ids.MonitorRunId runId = new Ids.MonitorRunId(result.getString("run_id"));
        Ids.MonitorOutputId outputId = Ids.MonitorOutputId.parse(
                result.getString("output_id")
        );
        MonitorOutputKind kind = MonitorOutputKind.fromCanonicalName(
                result.getString("output_kind")
        );
        MonitorOutput output = readOutput(connection, runId, outputId, kind);
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
                parseFingerprints(result.getString("affected_blocks_json")),
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

    private static void insertOutput(Connection connection, StoredMonitorRun run)
            throws SQLException {
        String table;
        String idColumn;
        if (run.output().kind() == MonitorOutputKind.FINDING) {
            table = "monitor_issues";
            idColumn = "issue_id";
        } else {
            table = "monitor_proposed_operations";
            idColumn = "proposal_id";
        }
        String sql = "INSERT INTO " + table + "(" + idColumn
                + ", run_id, payload_json) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, run.outputId().value());
            statement.setString(2, run.runId().value());
            statement.setString(3, CanonicalJson.string(run.output().canonicalValue()));
            statement.executeUpdate();
        }
    }

    private static MonitorOutput readOutput(
            Connection connection,
            Ids.MonitorRunId runId,
            Ids.MonitorOutputId outputId,
            MonitorOutputKind kind
    ) throws SQLException {
        String table = kind == MonitorOutputKind.FINDING
                ? "monitor_issues" : "monitor_proposed_operations";
        String idColumn = kind == MonitorOutputKind.FINDING
                ? "issue_id" : "proposal_id";
        String sql = "SELECT " + idColumn + ", payload_json FROM " + table
                + " WHERE run_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, runId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new StorageException(
                            "Stored monitor run is missing its output " + runId.value()
                    );
                }
                if (!outputId.value().equals(result.getString(idColumn))) {
                    throw new StorageException(
                            "Stored monitor output identity does not match its run"
                    );
                }
                return MonitorOutput.fromCanonical(parseObject(
                        result.getString("payload_json"), "monitor output"
                ));
            }
        }
    }

    private static List<MonitorBlockFingerprint> parseFingerprints(String json) {
        Object parsed = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), List.class
        );
        if (!(parsed instanceof List<?> values)) {
            throw new StorageException("Stored monitor affected blocks are not an array");
        }
        List<MonitorBlockFingerprint> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> raw)) {
                throw new StorageException("Stored monitor block fingerprint is not an object");
            }
            result.add(MonitorBlockFingerprint.fromCanonical(stringMap(raw)));
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> parseObject(String json, String path) {
        Object parsed = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), Map.class
        );
        if (!(parsed instanceof Map<?, ?> raw)) {
            throw new StorageException("Stored " + path + " is not an object");
        }
        return stringMap(raw);
    }

    private static Map<String, Object> stringMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new StorageException("Stored monitor JSON contains a non-string key");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}

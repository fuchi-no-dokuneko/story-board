package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class SqliteSecurityListAuditEvents {
    static List<AuditEvent> listAuditEvents(
            Connection connection,
            Ids.NovelId novelId
    ) throws SQLException {
        List<AuditEvent> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT event_id, occurred_at, request_id, actor_id, actor_key_id,
                       novel_id, action, subject_id, operation_id, revision_id,
                       result, operation_hash, content_hash, event_hash
                FROM audit_events
                WHERE novel_id = ?
                ORDER BY occurred_at, event_id
                """)) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    events.add(SqliteSecurityReadAuditEvent.readAuditEvent(result));
                }
            }
        }
        return List.copyOf(events);
    }
}

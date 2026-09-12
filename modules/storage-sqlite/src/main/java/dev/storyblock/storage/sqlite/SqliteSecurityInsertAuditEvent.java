package dev.storyblock.storage.sqlite;

import dev.storyblock.security.AuditEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteSecurityInsertAuditEvent {
    static void insertAuditEvent(Connection connection, AuditEvent event)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO audit_events(
                    event_id, occurred_at, request_id, actor_id, actor_key_id,
                    novel_id, action, subject_id, operation_id, revision_id,
                    result, operation_hash, content_hash, event_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, event.eventId().value());
            statement.setString(2, event.occurredAt().toString());
            statement.setString(3, event.requestId());
            statement.setString(4, event.actorId());
            statement.setString(5, event.actorKeyId() == null
                    ? null : event.actorKeyId().value());
            statement.setString(6, event.novelId().value());
            statement.setString(7, event.action().canonicalName());
            statement.setString(8, event.subjectId());
            statement.setString(9, event.operationId() == null
                    ? null : event.operationId().value());
            statement.setString(10, event.revisionId() == null
                    ? null : event.revisionId().value());
            statement.setString(11, event.result().canonicalName());
            statement.setString(12, event.operationHash());
            statement.setString(13, event.contentHash());
            statement.setString(14, event.eventHash());
            statement.executeUpdate();
        }
    }
}

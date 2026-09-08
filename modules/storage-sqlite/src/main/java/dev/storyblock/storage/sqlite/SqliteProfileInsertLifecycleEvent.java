package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.StyleLifecycleEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteProfileInsertLifecycleEvent {
    static void insertLifecycleEvent(
            Connection connection,
            StyleLifecycleEvent event
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profile_lifecycle_events(
                    event_id, profile_id, version_id, sequence, from_state, to_state,
                    event_json, request_id, actor_id, actor_key_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, event.eventId().value());
            statement.setString(2, event.profileId().value());
            statement.setString(3, event.versionId().value());
            statement.setInt(4, event.sequence());
            if (event.fromState() == null) {
                statement.setNull(5, java.sql.Types.VARCHAR);
            } else {
                statement.setString(5, event.fromState().canonicalName());
            }
            statement.setString(6, event.toState().canonicalName());
            statement.setString(7, CanonicalJson.string(event.canonicalValue()));
            statement.setString(8, event.auditContext().requestId());
            statement.setString(9, event.auditContext().actorId());
            if (event.auditContext().actorKeyId() == null) {
                statement.setNull(10, java.sql.Types.VARCHAR);
            } else {
                statement.setString(10, event.auditContext().actorKeyId().value());
            }
            statement.setString(11, event.occurredAt().toString());
            statement.executeUpdate();
        }
    }
}

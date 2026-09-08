package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.StyleLifecycleEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class SqliteProfileLifecycle {
    static List<StyleLifecycleEvent> lifecycle(
            Connection connection,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) throws SQLException {
        List<StyleLifecycleEvent> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT event_json, from_state, to_state, request_id, actor_id,
                       actor_key_id, occurred_at
                FROM style_profile_lifecycle_events
                WHERE profile_id = ? AND version_id = ?
                ORDER BY sequence
                """)) {
            statement.setString(1, profileId.value());
            statement.setString(2, versionId.value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    StyleLifecycleEvent event = SqliteProfileReadLifecycle.read(result, profileId, versionId);
                    events.add(event);
                }
            }
        }
        if (events.isEmpty()) {
            throw new StorageException("Stored style profile version has no lifecycle");
        }
        return List.copyOf(events);
    }
}

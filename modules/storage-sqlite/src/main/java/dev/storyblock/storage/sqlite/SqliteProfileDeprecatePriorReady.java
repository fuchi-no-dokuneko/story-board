package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.StyleLifecycleEvent;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileVersionView;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class SqliteProfileDeprecatePriorReady {
    static void deprecatePriorReady(
            Connection connection,
            StyleProfileVersionView promoted,
            AuditContext auditContext
    ) throws SQLException {
        List<Ids.StyleProfileVersionId> ready = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version.version_id
                FROM style_profile_versions AS version
                JOIN style_profile_lifecycle_events AS event
                  ON event.version_id = version.version_id
                WHERE version.profile_id = ?
                  AND version.version_id <> ?
                  AND event.sequence = (
                      SELECT MAX(current.sequence)
                      FROM style_profile_lifecycle_events AS current
                      WHERE current.version_id = version.version_id
                  )
                  AND event.to_state = 'ready'
                ORDER BY version.version
                """)) {
            statement.setString(1, promoted.profileVersion().profileId().value());
            statement.setString(2, promoted.profileVersion().versionId().value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ready.add(new Ids.StyleProfileVersionId(result.getString(1)));
                }
            }
        }
        if (ready.size() > 1) {
            throw new StorageException("Multiple active READY style versions were detected");
        }
        if (ready.isEmpty()) {
            return;
        }
        StyleProfileVersionView prior = SqliteProfileGetVersion.getVersion(
                connection, promoted.profileVersion().profileId(), ready.getFirst()
        );
        StyleLifecycleEvent deprecated = new StyleLifecycleEvent(
                Ids.StyleLifecycleEventId.create(),
                prior.profileVersion().profileId(),
                prior.profileVersion().versionId(),
                prior.lifecycle().size() + 1,
                StyleProfileState.READY,
                StyleProfileState.DEPRECATED,
                "Superseded by READY version "
                        + promoted.profileVersion().versionId().value(),
                false,
                auditContext,
                auditContext.occurredAt()
        );
        SqliteProfileInsertLifecycleEvent.insertLifecycleEvent(connection, deprecated);
    }
}

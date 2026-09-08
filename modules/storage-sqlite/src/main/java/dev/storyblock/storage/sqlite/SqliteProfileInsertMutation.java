package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileInsertMutation {
    static void insertMutation(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey,
            MutationKind kind,
            String requestHash,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            Ids.StyleLifecycleEventId eventId,
            AuditContext auditContext
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profile_mutations(
                    novel_id, idempotency_key, mutation_kind, request_hash,
                    profile_id, version_id, event_id, request_id, actor_id,
                    actor_key_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            statement.setString(3, kind.canonicalName());
            statement.setString(4, requestHash);
            statement.setString(5, profileId.value());
            if (versionId == null) {
                statement.setNull(6, java.sql.Types.VARCHAR);
            } else {
                statement.setString(6, versionId.value());
            }
            if (eventId == null) {
                statement.setNull(7, java.sql.Types.VARCHAR);
            } else {
                statement.setString(7, eventId.value());
            }
            statement.setString(8, auditContext.requestId());
            statement.setString(9, auditContext.actorId());
            if (auditContext.actorKeyId() == null) {
                statement.setNull(10, java.sql.Types.VARCHAR);
            } else {
                statement.setString(10, auditContext.actorKeyId().value());
            }
            statement.setString(11, auditContext.occurredAt().toString());
            statement.executeUpdate();
        }
    }
}

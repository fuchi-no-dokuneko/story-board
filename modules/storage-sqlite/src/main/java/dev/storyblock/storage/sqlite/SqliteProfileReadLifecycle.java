package dev.storyblock.storage.sqlite;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.StyleLifecycleEvent;
import java.sql.*;
final class SqliteProfileReadLifecycle {
    static StyleLifecycleEvent read(ResultSet result, Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId) throws SQLException {
        StyleLifecycleEvent event = StyleLifecycleEvent.fromCanonical(
                SqliteProfileParseObject.parseObject(result.getString("event_json"), "style lifecycle event")
        );
        String from = result.getString("from_state");
        String actorKey = result.getString("actor_key_id");
        if (!event.profileId().equals(profileId)
                || !event.versionId().equals(versionId)
                || !java.util.Objects.equals(
                        from,
                        event.fromState() == null
                                ? null : event.fromState().canonicalName()
                )
                || !event.toState().canonicalName().equals(
                        result.getString("to_state")
                )
                || !event.auditContext().requestId().equals(
                        result.getString("request_id")
                )
                || !event.auditContext().actorId().equals(
                        result.getString("actor_id")
                )
                || !java.util.Objects.equals(
                        actorKey,
                        event.auditContext().actorKeyId() == null
                                ? null : event.auditContext().actorKeyId().value()
                )
                || !event.occurredAt().toString().equals(
                        result.getString("occurred_at")
                )) {
            throw new StorageException(
                    "Stored style lifecycle event integrity check failed"
            );
        }
        return event;
    }
}

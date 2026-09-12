package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileFindMutation {
    static Optional<Mutation> findMutation(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT mutation_kind, request_hash, profile_id, version_id, event_id
                FROM style_profile_mutations
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String version = result.getString("version_id");
                String event = result.getString("event_id");
                return Optional.of(new Mutation(
                        MutationKind.fromCanonical(result.getString("mutation_kind")),
                        result.getString("request_hash"),
                        new Ids.StyleProfileId(result.getString("profile_id")),
                        version == null ? null : new Ids.StyleProfileVersionId(version),
                        event == null ? null : new Ids.StyleLifecycleEventId(event)
                ));
            }
        }
    }
}

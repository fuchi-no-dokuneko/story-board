package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.CreateStyleProfileCommand;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileSaveResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileCreateProfile {
    static StyleProfileSaveResult createProfile(
            Connection connection,
            CreateStyleProfileCommand command
    ) throws SQLException {
        Ids.NovelId novelId = command.profile().scope().novelId();
        Optional<Mutation> prior = SqliteProfileFindMutation.findMutation(
                connection, novelId, command.idempotencyKey()
        );
        if (prior.isPresent()) {
            Mutation replay = SqliteProfileRequireReplay.requireReplay(
                    prior.get(), MutationKind.CREATE_PROFILE, command.requestHash(),
                    command.idempotencyKey()
            );
            return new StyleProfileSaveResult(
                    SqliteProfileGetProfile.getProfile(connection, replay.profileId()), true
            );
        }

        StyleProfile profile = command.profile();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profiles(
                    profile_id, novel_id, profile_json, resource_hash, created_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, profile.profileId().value());
            statement.setString(2, novelId.value());
            statement.setString(3, CanonicalJson.string(profile.canonicalValue()));
            statement.setString(4, profile.resourceHash());
            statement.setString(5, profile.createdBy());
            statement.setString(6, profile.createdAt().toString());
            statement.executeUpdate();
        }
        SqliteProfileInsertMutation.insertMutation(
                connection,
                novelId,
                command.idempotencyKey(),
                MutationKind.CREATE_PROFILE,
                command.requestHash(),
                profile.profileId(),
                null,
                null,
                command.auditContext()
        );
        return new StyleProfileSaveResult(profile, false);
    }
}

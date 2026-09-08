package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileCreateVersion {
    static StyleProfileVersionSaveResult createVersion(
            Connection connection,
            CreateStyleProfileVersionCommand command
    ) throws SQLException {
        StyleProfile profile = SqliteProfileGetProfile.getProfile(connection, command.profileId());
        Ids.NovelId novelId = profile.scope().novelId();
        Optional<Mutation> prior = SqliteProfileFindMutation.findMutation(
                connection, novelId, command.idempotencyKey()
        );
        if (prior.isPresent()) {
            return SqliteProfileReplay.replay(connection, prior.get(), MutationKind.CREATE_VERSION,
                    command.requestHash(), command.idempotencyKey());
        }
        if (!profile.resourceHash().equals(command.expectedProfileHash())) {
            throw new StyleStatusPreconditionException(profile.resourceHash());
        }
        if (!profile.scope().equals(command.content().scope())) {
            throw new StyleLifecycleConflictException(
                    "Style profile version scope must exactly match its immutable profile scope"
            );
        }

        int number = SqliteProfileNextVersionNumber.nextVersionNumber(connection, profile.profileId());
        StyleProfileVersion version = new StyleProfileVersion(
                command.versionId(),
                profile.profileId(),
                number,
                command.content(),
                command.auditContext().actorId(),
                command.auditContext().occurredAt()
        );
        SqliteProfileInsertVersion.insertVersion(connection, version);
        StyleLifecycleEvent initial = StyleLifecycleEvent.initial(
                command.initialEventId(),
                profile.profileId(),
                version.versionId(),
                command.auditContext()
        );
        SqliteProfileInsertLifecycleEvent.insertLifecycleEvent(connection, initial);
        SqliteProfileInsertMutation.insertMutation(
                connection,
                novelId,
                command.idempotencyKey(),
                MutationKind.CREATE_VERSION,
                command.requestHash(),
                profile.profileId(),
                version.versionId(),
                initial.eventId(),
                command.auditContext()
        );
        return new StyleProfileVersionSaveResult(
                StyleProfileVersionView.of(version, List.of(initial)), false
        );
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileTransition {
    static StyleProfileVersionSaveResult transition(
            Connection connection,
            TransitionStyleProfileVersionCommand command
    ) throws SQLException {
        StyleProfile profile = SqliteProfileGetProfile.getProfile(connection, command.profileId());
        Ids.NovelId novelId = profile.scope().novelId();
        Optional<Mutation> prior = SqliteProfileFindMutation.findMutation(
                connection, novelId, command.idempotencyKey()
        );
        if (prior.isPresent()) {
            return SqliteProfileReplay.replay(connection, prior.get(), MutationKind.TRANSITION,
                    command.requestHash(), command.idempotencyKey());
        }

        StyleProfileVersionView current = SqliteProfileGetVersion.getVersion(
                connection, command.profileId(), command.versionId()
        );
        SqliteProfileRequireTransition.require(current, command);
        if (command.targetState() == StyleProfileState.READY) {
            SqliteProfileDeprecatePriorReady.deprecatePriorReady(connection, current, command.auditContext());
        }
        StyleLifecycleEvent event = new StyleLifecycleEvent(
                command.eventId(),
                command.profileId(),
                command.versionId(),
                current.lifecycle().size() + 1,
                current.state(),
                command.targetState(),
                command.reason(),
                command.targetState() == StyleProfileState.READY
                        && command.confirmGeneratedCorpusPromotion(),
                command.auditContext(),
                command.auditContext().occurredAt()
        );
        SqliteProfileInsertLifecycleEvent.insertLifecycleEvent(connection, event);
        SqliteProfileInsertMutation.insertMutation(
                connection,
                novelId,
                command.idempotencyKey(),
                MutationKind.TRANSITION,
                command.requestHash(),
                command.profileId(),
                command.versionId(),
                event.eventId(),
                command.auditContext()
        );
        List<StyleLifecycleEvent> lifecycle = new ArrayList<>(current.lifecycle());
        lifecycle.add(event);
        return new StyleProfileVersionSaveResult(
                StyleProfileVersionView.of(current.profileVersion(), lifecycle), false
        );
    }
}

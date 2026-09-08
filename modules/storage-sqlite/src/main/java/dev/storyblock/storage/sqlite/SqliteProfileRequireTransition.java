package dev.storyblock.storage.sqlite;

import dev.storyblock.style.*;
import java.sql.SQLException;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileRequireTransition {
    static void require(StyleProfileVersionView current, TransitionStyleProfileVersionCommand command) throws SQLException {
        if (!current.statusHash().equals(command.expectedStatusHash())) {
            throw new StyleStatusPreconditionException(current.statusHash());
        }
        if (!current.state().canTransitionTo(command.targetState())) {
            throw new StyleLifecycleConflictException(
                    "Style profile lifecycle must follow DRAFT -> CALIBRATING -> READY "
                            + "-> DEPRECATED"
            );
        }
        if (command.targetState() == StyleProfileState.READY
                && current.profileVersion().content().containsGeneratedText()
                && !command.confirmGeneratedCorpusPromotion()) {
            throw new StyleLifecycleConflictException(
                    "Generated or mixed corpus requires explicit promotion confirmation"
            );
        }

    }
}

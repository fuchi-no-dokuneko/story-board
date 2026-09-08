package dev.storyblock.storage.sqlite;

import dev.storyblock.rewrite.policy.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteReservationData.*;

final class SqliteReservationReserve {
    static RewriteCandidateReservationSaveResult reserve(
            Connection connection,
            ReserveRewriteCandidateCommand command
    ) throws SQLException {
        RewriteCandidateReservation candidate = command.reservation();
        Optional<StoredReservation> prior = SqliteReservationFindByIdempotency.findByIdempotency(
                connection, candidate.novelId(), command.idempotencyKey()
        );
        if (prior.isPresent()) {
            if (!prior.get().requestHash().equals(command.requestHash())) {
                throw new RewriteReservationConflictException();
            }
            return new RewriteCandidateReservationSaveResult(
                    prior.get().reservation(), true
            );
        }
        SqliteReservationRequireUnreservedFindings.requireUnreservedFindings(connection, candidate);
        SqliteReservationRequireCooldownAvailable.requireCooldownAvailable(connection, candidate);
        SqliteReservationInsertReservation.insertReservation(connection, command);
        SqliteReservationInsertFindings.insertFindings(connection, candidate);
        SqliteReservationInsertBlocks.insertBlocks(connection, candidate);
        return new RewriteCandidateReservationSaveResult(candidate, false);
    }
}

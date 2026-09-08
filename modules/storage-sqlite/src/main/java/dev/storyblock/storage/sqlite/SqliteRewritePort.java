package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.util.Objects;

interface SqliteRewritePort extends RewriteReservationStore, SqliteStoreContext {
  @Override
  default RewriteCandidateReservationSaveResult reserveRewriteCandidate(
      ReserveRewriteCandidateCommand command
  ) {
    Objects.requireNonNull(command, "command");
    return context().write(connection -> SqliteReservationReserve.reserve(
        connection, command
    ));
  }

  @Override
  default RewriteCandidateReservation getRewriteCandidateReservation(
      Ids.ProposalId proposalId
  ) {
    Objects.requireNonNull(proposalId, "proposalId");
    return context().read(connection -> SqliteReservationGet.get(
        connection, proposalId
    ));
  }
}

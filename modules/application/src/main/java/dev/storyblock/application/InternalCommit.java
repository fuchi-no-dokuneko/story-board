package dev.storyblock.application;

import dev.storyblock.domain.*;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.*;
import java.time.Instant;
import java.util.Objects;

final class InternalCommit {
  static CommitResult commit(CommitService self, EditOperation requestedOperation, Ids.RevisionId candidateRevisionId, Instant committedAt)  {
    Objects.requireNonNull(requestedOperation, "requestedOperation");
    return self.commit(
        requestedOperation,
        candidateRevisionId,
        committedAt,
        AuditContext.system(
            "req_internal_" + requestedOperation.context().operationId().value(),
            committedAt
        )
    );
  }

}

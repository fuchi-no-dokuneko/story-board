package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.CommitResult;
import java.time.Instant;

final class CommitServiceCommitAction {
    static CommitResult commit(CommitService self, EditOperation requestedOperation, Ids.RevisionId candidateRevisionId, Instant committedAt)  {
        return CommitServiceCommitActionCommitFactory.commit(self, requestedOperation, candidateRevisionId, committedAt);
    }

    static CommitResult commit(CommitService self, EditOperation requestedOperation, Ids.RevisionId candidateRevisionId, Instant committedAt, AuditContext auditContext)  {
        return CommitServiceCommitActionCommitFactory.commit(self, requestedOperation, candidateRevisionId, committedAt, auditContext);
    }
}

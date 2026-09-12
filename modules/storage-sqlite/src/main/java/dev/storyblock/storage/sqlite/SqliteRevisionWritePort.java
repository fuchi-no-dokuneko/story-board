package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.util.Objects;

interface SqliteRevisionWritePort extends RevisionStore, SqliteStoreContext {
  @Override
  default void createNovel(RevisionManifest initialRevision, String contentHash) {
    SqliteRevisionStoreCreateNovelAction.createNovel(context(), initialRevision, contentHash);
  }

  @Override
  default CommitResult commitCas(CommitRequest request) {
    Objects.requireNonNull(request, "request");
    return commitCas(
        request,
        AuditContext.system(
            "req_internal_" + request.operation().context().operationId().value(),
            request.candidate().createdAt()
        )
    );
  }

  @Override
  default CommitResult commitCas(CommitRequest request, AuditContext auditContext) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(auditContext, "auditContext");
    SqliteRevisionStoreVerifyRequestHashes.verifyRequestHashes(request);
    return context().write(connection -> SqliteRevisionStoreCommitAction.commit(context(), connection, request, auditContext));
  }

  @Override
  default void recordCommitReplayAudit(
      StoredOperation operation,
      AuditContext auditContext
  ) {
    SqliteRevisionStoreRecordCommitReplayAuditAction.recordCommitReplayAudit(context(), operation, auditContext);
  }
}

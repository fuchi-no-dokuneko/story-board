package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

final class SqliteRevisionStoreCommitAction {
  static CommitResult commit(SqliteRevisionStore self, Connection connection, CommitRequest request, AuditContext auditContext) throws SQLException {
    Ids.NovelId novelId = request.operation().context().novelId();
    Optional<StoredOperation> prior = SqliteRevisionStoreFindByIdempotencyKeyFactory.findByIdempotencyKey(
        connection, novelId, request.operation().context().idempotencyKey()
    );
    self.faultInjector.after(CommitStage.AFTER_IDEMPOTENCY_CHECK);
    if (prior.isPresent()) {
      StoredOperation stored = prior.get();
      if (!stored.operationHash().equals(request.operationHash())) {
        throw new IdempotencyConflictException(
            request.operation().context().idempotencyKey(),
            stored.operationHash(),
            request.operationHash()
        );
      }
      CommitResult result = new CommitResult(
          new RevisionRef(
              stored.resultRevisionId(), stored.sequence(), stored.resultHash()
          ),
          stored.operation().context().operationId(),
          true
      );
      SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, SqliteRevisionStoreCommitAuditEvent.commitAuditEvent(
          novelId,
          stored.operation().context().operationId(),
          stored.resultRevisionId(),
          stored.operationHash(),
          stored.resultHash(),
          AuditResult.IDEMPOTENT,
          auditContext
      ));
      self.faultInjector.after(CommitStage.AFTER_AUDIT);
      return result;
    }

    RevisionRef actualHead = SqliteRevisionStoreRequireHead.requireHead(connection, novelId);
    if (!actualHead.equals(request.expectedHead())) {
      throw new StaleHeadException(request.expectedHead(), actualHead);
    }
    long sequence = actualHead.sequence() + 1;
    byte[] operationBytes = CanonicalJson.bytes(
        EditOperationCanonicalMapper.toCanonical(request.operation())
    );
    byte[] candidateBytes = NarrativeCanonicalMapper.toCanonical(request.candidate()).envelopeBytes();

    SqliteRevisionStoreInsertOperation.insertOperation(connection, request, sequence, operationBytes);
    self.faultInjector.after(CommitStage.AFTER_OPERATION_APPEND);
    SqliteRevisionStoreInsertRevision.insertRevision(
        connection,
        request.candidate(),
        sequence,
        request.candidateHash(),
        candidateBytes,
        request.operation().context().operationId()
    );
    self.faultInjector.after(CommitStage.AFTER_REVISION_APPEND);
    SqliteRevisionStoreInsertTombstonesAction.insertTombstones(self, connection, request);
    self.faultInjector.after(CommitStage.AFTER_TOMBSTONES);
    SqliteRevisionStoreRebuildProjection.rebuildProjection(connection, request.candidate());
    self.faultInjector.after(CommitStage.AFTER_PROJECTION);
    if (SqliteRevisionStoreShouldCheckpointAction.shouldCheckpoint(self, connection, novelId, sequence)) {
      SqliteRevisionStoreInsertCheckpoint.insertCheckpoint(
          connection,
          request.candidate(),
          sequence,
          request.candidateHash(),
          candidateBytes
      );
    }
    self.faultInjector.after(CommitStage.AFTER_CHECKPOINT);
    self.faultInjector.after(CommitStage.BEFORE_HEAD_CAS);

    try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE novels
                SET head_revision_id = ?, head_sequence = ?, head_hash = ?
                WHERE novel_id = ? AND head_revision_id = ? AND head_hash = ?
                """)) {
      statement.setString(1, request.candidate().id().value());
      statement.setLong(2, sequence);
      statement.setString(3, request.candidateHash());
      statement.setString(4, novelId.value());
      statement.setString(5, request.expectedHead().revisionId().value());
      statement.setString(6, request.expectedHead().contentHash());
      if (statement.executeUpdate() != 1) {
        throw new StaleHeadException(
            request.expectedHead(), SqliteRevisionStoreRequireHead.requireHead(connection, novelId)
        );
      }
    }
    SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, SqliteRevisionStoreCommitAuditEvent.commitAuditEvent(
        novelId,
        request.operation().context().operationId(),
        request.candidate().id(),
        request.operationHash(),
        request.candidateHash(),
        AuditResult.SUCCEEDED,
        auditContext
    ));
    self.faultInjector.after(CommitStage.AFTER_AUDIT);
    return new CommitResult(
        new RevisionRef(request.candidate().id(), sequence, request.candidateHash()),
        request.operation().context().operationId(),
        false
    );
  }
}

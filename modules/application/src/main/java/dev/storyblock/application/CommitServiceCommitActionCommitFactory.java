package dev.storyblock.application;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.*;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

final class CommitServiceCommitActionCommitFactory {
  static CommitResult commit(CommitService self, EditOperation requestedOperation, Ids.RevisionId candidateRevisionId, Instant committedAt, AuditContext auditContext)  {
    Objects.requireNonNull(requestedOperation, "requestedOperation");
    Objects.requireNonNull(candidateRevisionId, "candidateRevisionId");
    Objects.requireNonNull(committedAt, "committedAt");
    Objects.requireNonNull(auditContext, "auditContext");

    EditOperation operation = EditOperationNormalizer.normalize(requestedOperation);
    String operationHash = EditOperationCanonicalMapper.hash(operation);
    Optional<StoredOperation> prior = self.store.findByIdempotencyKey(
        operation.context().novelId(), operation.context().idempotencyKey()
    );
    if (prior.isPresent()) {
      CommitResult result = CommitServicePriorResult.priorResult(operation, operationHash, prior.get());
      self.store.recordCommitReplayAudit(prior.get(), auditContext);
      return result;
    }

    StoredRevision base = self.store.getRevision(
        operation.context().novelId(), operation.context().baseRevisionId()
    );
    RevisionRef expectedHead = new RevisionRef(
        base.manifest().id(), base.sequence(), operation.context().expectedHeadHash()
    );
    RevisionRef actualHead = self.store.getHead(operation.context().novelId());
    if (!expectedHead.equals(actualHead)) {
      throw new StaleHeadException(expectedHead, actualHead);
    }
    self.validateImageReferences(operation);

    RevisionLookup lookup = revisionId -> self.store.getRevision(
        operation.context().novelId(), revisionId
    ).manifest();
    PreviewResponse preview = new PreviewService(lookup).preview(
        base.manifest(), operation, candidateRevisionId, committedAt
    );
    if (!preview.committable()) {
      throw new CommitRejectedException(preview);
    }

    RevisionManifest candidate = new NarrativeEditor(lookup).apply(
        base.manifest(), operation, candidateRevisionId, committedAt
    );
    String candidateHash = NarrativeCanonicalMapper.toCanonical(candidate).contentHash();
    if (!candidateHash.equals(preview.candidateHash())) {
      throw new IllegalStateException("Preview and commit candidate hashes diverged");
    }

    return self.store.commitCas(
        new CommitRequest(
            expectedHead, operation, operationHash, candidate, candidateHash
        ),
        auditContext
    );
  }
}

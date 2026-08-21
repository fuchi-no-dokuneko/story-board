package dev.storyblock.application;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.CommitRequest;
import dev.storyblock.storage.CommitResult;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredOperation;
import dev.storyblock.storage.StoredRevision;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class CommitService {
    private final RevisionStore store;

    public CommitService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public CommitResult commit(
            EditOperation requestedOperation,
            Ids.RevisionId candidateRevisionId,
            Instant committedAt
    ) {
        Objects.requireNonNull(requestedOperation, "requestedOperation");
        Objects.requireNonNull(candidateRevisionId, "candidateRevisionId");
        Objects.requireNonNull(committedAt, "committedAt");

        EditOperation operation = EditOperationNormalizer.normalize(requestedOperation);
        String operationHash = EditOperationCanonicalMapper.hash(operation);
        Optional<StoredOperation> prior = store.findByIdempotencyKey(
                operation.context().novelId(), operation.context().idempotencyKey()
        );
        if (prior.isPresent()) {
            return priorResult(operation, operationHash, prior.get());
        }

        StoredRevision base = store.getRevision(
                operation.context().novelId(), operation.context().baseRevisionId()
        );
        RevisionRef expectedHead = new RevisionRef(
                base.manifest().id(), base.sequence(), operation.context().expectedHeadHash()
        );
        RevisionRef actualHead = store.getHead(operation.context().novelId());
        if (!expectedHead.equals(actualHead)) {
            throw new StaleHeadException(expectedHead, actualHead);
        }

        RevisionLookup lookup = revisionId -> store.getRevision(
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

        return store.commitCas(new CommitRequest(
                expectedHead, operation, operationHash, candidate, candidateHash
        ));
    }

    private static CommitResult priorResult(
            EditOperation operation,
            String operationHash,
            StoredOperation prior
    ) {
        if (!prior.operationHash().equals(operationHash)) {
            throw new IdempotencyConflictException(
                    operation.context().idempotencyKey(),
                    prior.operationHash(),
                    operationHash
            );
        }
        return new CommitResult(
                new RevisionRef(
                        prior.resultRevisionId(), prior.sequence(), prior.resultHash()
                ),
                prior.operation().context().operationId(),
                true
        );
    }
}

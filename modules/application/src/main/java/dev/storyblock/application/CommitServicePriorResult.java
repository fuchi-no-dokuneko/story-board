package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.storage.CommitResult;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StoredOperation;

final class CommitServicePriorResult {
    static CommitResult priorResult(
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

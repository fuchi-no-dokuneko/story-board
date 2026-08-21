package dev.storyblock.storage;

import dev.storyblock.domain.Ids;
import java.util.Objects;

public record CommitResult(
        RevisionRef revision,
        Ids.OperationId operationId,
        boolean idempotentReplay
) {
    public CommitResult {
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(operationId, "operationId");
    }
}

package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;

public interface CanonicalRevisionTypes {
    public record RevisionEntry(long sequence, CanonicalRevision revision) {
        public RevisionEntry {
            if (sequence < 0) {
                throw new CanonicalPackageException("Revision sequence cannot be negative");
            }
            Objects.requireNonNull(revision, "revision");
        }
    }

    public record OperationEntry(
            long sequence,
            String operationHash,
            EditOperation operation,
            Ids.RevisionId resultRevisionId,
            String resultHash,
            Instant committedAt
    ) {
        public OperationEntry {
            if (sequence < 1) {
                throw new CanonicalPackageException("Operation sequence must be positive");
            }
            CanonicalNovelPackageRequireHash.requireHash(operationHash, "Operation hash");
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(resultRevisionId, "resultRevisionId");
            CanonicalNovelPackageRequireHash.requireHash(resultHash, "Operation result hash");
            Objects.requireNonNull(committedAt, "committedAt");
        }
    }
}

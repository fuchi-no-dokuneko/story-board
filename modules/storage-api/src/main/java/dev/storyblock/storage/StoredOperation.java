package dev.storyblock.storage;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record StoredOperation(
        EditOperation operation,
        long sequence,
        String operationHash,
        Ids.RevisionId resultRevisionId,
        String resultHash,
        Instant committedAt
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public StoredOperation {
        Objects.requireNonNull(operation, "operation");
        if (sequence < 1) {
            throw new IllegalArgumentException("Operation sequence must be positive");
        }
        requireHash(operationHash, "Operation hash");
        Objects.requireNonNull(resultRevisionId, "resultRevisionId");
        requireHash(resultHash, "Operation result hash");
        Objects.requireNonNull(committedAt, "committedAt");
    }

    private static void requireHash(String value, String field) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
    }
}

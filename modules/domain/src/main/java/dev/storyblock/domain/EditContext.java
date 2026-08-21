package dev.storyblock.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record EditContext(
        Ids.OperationId operationId,
        String idempotencyKey,
        Ids.NovelId novelId,
        Ids.RevisionId baseRevisionId,
        String expectedHeadHash
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public EditContext {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(baseRevisionId, "baseRevisionId");
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("Idempotency key must contain 1 to 200 characters");
        }
        if (expectedHeadHash == null || !SHA_256.matcher(expectedHeadHash).matches()) {
            throw new IllegalArgumentException("Expected head hash must be lowercase SHA-256");
        }
    }
}

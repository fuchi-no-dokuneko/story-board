package dev.storyblock.storage;

import dev.storyblock.contracts.CanonicalNovelPackage;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record CanonicalImportRequest(
        CanonicalNovelPackage document,
        String idempotencyKey,
        String requestHash,
        Instant importedAt
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public CanonicalImportRequest {
        Objects.requireNonNull(document, "document");
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("Import idempotency key is invalid");
        }
        if (requestHash == null || !SHA_256.matcher(requestHash).matches()) {
            throw new IllegalArgumentException("Import request hash must be lowercase SHA-256");
        }
        Objects.requireNonNull(importedAt, "importedAt");
    }
}

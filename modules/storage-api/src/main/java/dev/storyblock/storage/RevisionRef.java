package dev.storyblock.storage;

import dev.storyblock.domain.Ids;
import java.util.Objects;
import java.util.regex.Pattern;

public record RevisionRef(Ids.RevisionId revisionId, long sequence, String contentHash) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public RevisionRef {
        Objects.requireNonNull(revisionId, "revisionId");
        if (sequence < 0) {
            throw new IllegalArgumentException("Revision sequence cannot be negative");
        }
        if (contentHash == null || !SHA_256.matcher(contentHash).matches()) {
            throw new IllegalArgumentException("Revision content hash must be lowercase SHA-256");
        }
    }
}

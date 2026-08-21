package dev.storyblock.storage;

import dev.storyblock.domain.Ids;
import java.util.Objects;
import java.util.regex.Pattern;

public record StoredCheckpoint(
        Ids.NovelId novelId,
        Ids.RevisionId revisionId,
        long sequence,
        String contentHash,
        String codec,
        int uncompressedBytes,
        byte[] compressedCanonicalJson
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public StoredCheckpoint {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        if (sequence < 0) {
            throw new IllegalArgumentException("Checkpoint sequence cannot be negative");
        }
        if (contentHash == null || !SHA_256.matcher(contentHash).matches()) {
            throw new IllegalArgumentException("Checkpoint hash must be lowercase SHA-256");
        }
        if (codec == null || codec.isBlank()) {
            throw new IllegalArgumentException("Checkpoint codec cannot be blank");
        }
        if (uncompressedBytes < 1) {
            throw new IllegalArgumentException("Checkpoint uncompressed size must be positive");
        }
        compressedCanonicalJson = compressedCanonicalJson.clone();
        if (compressedCanonicalJson.length == 0) {
            throw new IllegalArgumentException("Checkpoint payload cannot be empty");
        }
    }

    @Override
    public byte[] compressedCanonicalJson() {
        return compressedCanonicalJson.clone();
    }
}

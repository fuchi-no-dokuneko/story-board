package dev.storyblock.application;

import dev.storyblock.domain.RevisionManifest;
import java.util.Objects;
import java.util.regex.Pattern;

public record ReplayResult(
        RevisionManifest revision,
        String contentHash,
        long targetSequence,
        long startingSequence,
        long replayedOperations
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public ReplayResult {
        Objects.requireNonNull(revision, "revision");
        if (contentHash == null || !SHA_256.matcher(contentHash).matches()) {
            throw new IllegalArgumentException("Replay hash must be lowercase SHA-256");
        }
        if (targetSequence < 0 || startingSequence < 0 || startingSequence > targetSequence) {
            throw new IllegalArgumentException("Replay sequence range is invalid");
        }
        if (replayedOperations != targetSequence - startingSequence) {
            throw new IllegalArgumentException("Replay operation count does not match its range");
        }
    }
}

package dev.storyblock.storage;

import java.util.Objects;

public record PortableArtifactPutResult(
        StoredArtifact artifact,
        boolean idempotentReplay
) {
    public PortableArtifactPutResult {
        Objects.requireNonNull(artifact, "artifact");
    }
}

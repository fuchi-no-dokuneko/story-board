package dev.storyblock.storage;

import java.util.Objects;

public record PortableArtifactPutRequest(
        RevisionRef expectedHead,
        String idempotencyKey,
        StoredArtifact artifact
) {
    public PortableArtifactPutRequest {
        Objects.requireNonNull(expectedHead, "expectedHead");
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("Artifact idempotency key is invalid");
        }
        Objects.requireNonNull(artifact, "artifact");
        if (!artifact.portable()
                || !artifact.revisionId().equals(expectedHead.revisionId())) {
            throw new IllegalArgumentException(
                    "Portable artifact must belong to the expected head revision"
            );
        }
    }
}

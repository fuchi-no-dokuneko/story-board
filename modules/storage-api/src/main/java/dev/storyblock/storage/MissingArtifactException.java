package dev.storyblock.storage;

import dev.storyblock.domain.Ids;

public final class MissingArtifactException extends StorageException {
    public MissingArtifactException(Ids.ArtifactId artifactId) {
        super("No stored artifact " + artifactId.value());
    }
}

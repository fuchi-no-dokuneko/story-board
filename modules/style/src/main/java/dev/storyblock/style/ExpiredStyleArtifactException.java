package dev.storyblock.style;

import dev.storyblock.domain.Ids;

public final class ExpiredStyleArtifactException extends RuntimeException {
    public ExpiredStyleArtifactException(Ids.ArtifactId artifactId) {
        super("Style analysis artifact has expired: " + artifactId.value());
    }
}

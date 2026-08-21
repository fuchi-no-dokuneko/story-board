package dev.storyblock.style;

import dev.storyblock.domain.Ids;

public final class MissingStyleProfileVersionException extends RuntimeException {
    public MissingStyleProfileVersionException(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) {
        super("Style profile version does not exist: "
                + profileId.value() + "/" + versionId.value());
    }
}

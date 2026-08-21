package dev.storyblock.style;

import dev.storyblock.domain.Ids;

public final class MissingStyleProfileException extends RuntimeException {
    public MissingStyleProfileException(Ids.StyleProfileId profileId) {
        super("Style profile does not exist: " + profileId.value());
    }
}

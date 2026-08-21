package dev.storyblock.storage;

import dev.storyblock.domain.Ids;

public final class NovelConflictException extends StorageException {
    public NovelConflictException(Ids.NovelId novelId) {
        super("Novel already exists " + novelId.value());
    }
}

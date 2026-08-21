package dev.storyblock.storage;

import dev.storyblock.domain.Ids;

public final class MissingNovelException extends StorageException {
    public MissingNovelException(Ids.NovelId novelId) {
        super("No stored novel " + novelId.value());
    }
}

package dev.storyblock.storage;

import dev.storyblock.domain.Ids;

public final class MissingRevisionException extends StorageException {
    public MissingRevisionException(Ids.NovelId novelId, Ids.RevisionId revisionId) {
        super("Novel " + novelId.value() + " has no revision " + revisionId.value());
    }
}

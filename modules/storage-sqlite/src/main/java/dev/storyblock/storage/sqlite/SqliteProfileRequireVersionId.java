package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileRequireVersionId {
    static Ids.StyleProfileVersionId requireVersionId(Mutation mutation) {
        if (mutation.versionId() == null) {
            throw new StorageException("Style mutation is missing its version identity");
        }
        return mutation.versionId();
    }
}

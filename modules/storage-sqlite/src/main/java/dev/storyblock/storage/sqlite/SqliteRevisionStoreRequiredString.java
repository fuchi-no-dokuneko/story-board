package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import java.util.Map;

final class SqliteRevisionStoreRequiredString {
    static String requiredString(Map<String, Object> value, String field) {
        Object entry = value.get(field);
        if (!(entry instanceof String string)) {
            throw new StorageException("Stored tombstone field " + field + " is not a string");
        }
        return string;
    }
}

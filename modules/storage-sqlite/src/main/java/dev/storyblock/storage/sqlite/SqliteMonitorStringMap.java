package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import java.util.LinkedHashMap;
import java.util.Map;

final class SqliteMonitorStringMap {
    static Map<String, Object> stringMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new StorageException("Stored monitor JSON contains a non-string key");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}

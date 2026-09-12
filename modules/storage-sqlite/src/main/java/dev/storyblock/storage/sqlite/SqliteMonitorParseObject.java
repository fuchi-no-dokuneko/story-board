package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.storage.StorageException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class SqliteMonitorParseObject {
    static Map<String, Object> parseObject(String json, String path) {
        Object parsed = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), Map.class
        );
        if (!(parsed instanceof Map<?, ?> raw)) {
            throw new StorageException("Stored " + path + " is not an object");
        }
        return SqliteMonitorStringMap.stringMap(raw);
    }
}

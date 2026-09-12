package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.monitor.MonitorBlockFingerprint;
import dev.storyblock.storage.StorageException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SqliteMonitorParseFingerprints {
    static List<MonitorBlockFingerprint> parseFingerprints(String json) {
        Object parsed = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), List.class
        );
        if (!(parsed instanceof List<?> values)) {
            throw new StorageException("Stored monitor affected blocks are not an array");
        }
        List<MonitorBlockFingerprint> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> raw)) {
                throw new StorageException("Stored monitor block fingerprint is not an object");
            }
            result.add(MonitorBlockFingerprint.fromCanonical(SqliteMonitorStringMap.stringMap(raw)));
        }
        return List.copyOf(result);
    }
}

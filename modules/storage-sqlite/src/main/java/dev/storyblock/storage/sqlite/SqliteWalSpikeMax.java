package dev.storyblock.storage.sqlite;

import java.util.List;
import java.util.Properties;

final class SqliteWalSpikeMax {
    static long max(List<Properties> results, String key) {
        return results.stream().mapToLong(result -> SqliteWalSpikeProperty.property(result, key)).max().orElse(0L);
    }
}

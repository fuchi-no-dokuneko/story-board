package dev.storyblock.storage.sqlite;

import java.util.List;
import java.util.Properties;

final class SqliteWalSpikeSum {
    static long sum(List<Properties> results, String key) {
        return results.stream().mapToLong(result -> SqliteWalSpikeProperty.property(result, key)).sum();
    }
}

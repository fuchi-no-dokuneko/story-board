package dev.storyblock.storage.sqlite;

import java.util.Properties;

final class SqliteWalSpikeProperty {
    static long property(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Worker result is missing " + key);
        }
        return Long.parseLong(value);
    }
}

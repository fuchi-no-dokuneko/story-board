package dev.storyblock.storage.sqlite;

public record SqliteSettings(int maximumPoolSize, int busyTimeoutMillis) {
    public static final int REQUIRED_BUSY_TIMEOUT_MILLIS = 5_000;
    public static final int REQUIRED_MAXIMUM_POOL_SIZE = 4;
    public static final SqliteSettings DEFAULT = new SqliteSettings(
            REQUIRED_MAXIMUM_POOL_SIZE,
            REQUIRED_BUSY_TIMEOUT_MILLIS
    );

    public SqliteSettings {
        if (maximumPoolSize < 1 || maximumPoolSize > REQUIRED_MAXIMUM_POOL_SIZE) {
            throw new IllegalArgumentException("SQLite pool size must be between 1 and 4");
        }
        if (busyTimeoutMillis < 0 || busyTimeoutMillis > 60_000) {
            throw new IllegalArgumentException("SQLite busy timeout must be between 0 and 60000 ms");
        }
    }
}

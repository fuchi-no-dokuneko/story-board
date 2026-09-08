package dev.storyblock.storage.sqlite;

import java.util.concurrent.TimeUnit;

final class SqliteMetricsNanosToMillis {
    static long nanosToMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }
}

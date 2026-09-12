package dev.storyblock.storage.sqlite;

import java.time.Instant;

final class SqliteAnalysisNullableInstant {
    static Instant nullableInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}

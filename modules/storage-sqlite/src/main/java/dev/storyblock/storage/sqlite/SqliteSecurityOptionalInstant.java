package dev.storyblock.storage.sqlite;

import java.time.Instant;

final class SqliteSecurityOptionalInstant {
    static Instant optionalInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.security.StoredAccessKey;

final class SqliteSecurityData {
    record StoredIssue(StoredAccessKey key, String requestHash) {
    }
}

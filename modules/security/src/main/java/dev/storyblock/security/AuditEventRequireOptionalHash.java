package dev.storyblock.security;

import static dev.storyblock.security.AuditEvent.HASH;

final class AuditEventRequireOptionalHash {
    static void requireOptionalHash(String value, String field) {
        if (value != null && !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
    }
}

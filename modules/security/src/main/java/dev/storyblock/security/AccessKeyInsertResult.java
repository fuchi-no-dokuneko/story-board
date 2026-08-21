package dev.storyblock.security;

import java.util.Objects;

public record AccessKeyInsertResult(
        StoredAccessKey key,
        boolean idempotentReplay
) {
    public AccessKeyInsertResult {
        Objects.requireNonNull(key, "key");
    }
}

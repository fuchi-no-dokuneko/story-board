package dev.storyblock.security;

import dev.storyblock.domain.Ids;

public final class MissingAccessKeyException extends RuntimeException {
    public MissingAccessKeyException(Ids.AccessKeyId keyId) {
        super("Access key not found: " + keyId.value());
    }
}

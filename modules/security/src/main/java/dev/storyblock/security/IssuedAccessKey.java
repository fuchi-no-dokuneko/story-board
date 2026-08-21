package dev.storyblock.security;

import java.util.Objects;

public record IssuedAccessKey(StoredAccessKey key, String bearerToken) {
    public IssuedAccessKey {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(bearerToken, "bearerToken");
    }

    @Override
    public String toString() {
        return "IssuedAccessKey[keyId=" + key.keyId().value()
                + ", novelId=" + key.novelId().value()
                + ", bearerToken=<redacted>]";
    }
}

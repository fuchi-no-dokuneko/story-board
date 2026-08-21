package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record StoredAccessKey(
        Ids.AccessKeyId keyId,
        Ids.NovelId novelId,
        byte[] secretDigest,
        Set<AccessScope> scopes,
        String actorId,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt
) {
    public StoredAccessKey {
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(novelId, "novelId");
        secretDigest = Objects.requireNonNull(secretDigest, "secretDigest").clone();
        if (secretDigest.length != 32) {
            throw new IllegalArgumentException("Access-key digest must contain 256 bits");
        }
        scopes = Set.copyOf(scopes);
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("Access key needs at least one scope");
        }
        actorId = SecurityIdentifier.require(actorId, "Actor ID");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Access-key expiry must follow creation");
        }
        if (revokedAt != null && revokedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Access-key revocation predates creation");
        }
        if (lastUsedAt != null && lastUsedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Access-key use predates creation");
        }
    }

    @Override
    public byte[] secretDigest() {
        return secretDigest.clone();
    }

    public boolean activeAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant);
    }
}

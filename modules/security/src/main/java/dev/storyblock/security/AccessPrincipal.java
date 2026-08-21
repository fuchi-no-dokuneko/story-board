package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Set;

public record AccessPrincipal(
        String actorId,
        Ids.AccessKeyId keyId,
        Ids.NovelId novelId,
        Set<AccessScope> scopes,
        Instant expiresAt,
        boolean owner
) {
    public AccessPrincipal {
        actorId = SecurityIdentifier.require(actorId, "Actor ID");
        scopes = Set.copyOf(scopes);
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("An access principal needs at least one scope");
        }
        if (owner) {
            if (keyId != null || novelId != null || expiresAt != null) {
                throw new IllegalArgumentException("Owner principal cannot be novel-bound");
            }
        } else if (keyId == null || novelId == null || expiresAt == null) {
            throw new IllegalArgumentException("Access-key principal must be novel-bound");
        }
    }

    public static AccessPrincipal ownerPrincipal() {
        return new AccessPrincipal(
                "owner", null, null, Set.of(AccessScope.values()), null, true
        );
    }

    public boolean canAccess(Ids.NovelId requestedNovelId) {
        return owner || novelId.equals(requestedNovelId);
    }
}

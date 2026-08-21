package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Set;

public record IssueAccessKeyCommand(
        Ids.NovelId novelId,
        String actorId,
        Set<AccessScope> scopes,
        Instant expiresAt,
        String idempotencyKey,
        AuditContext auditContext
) {
    public IssueAccessKeyCommand {
        if (novelId == null) {
            throw new IllegalArgumentException("Novel ID is required");
        }
        actorId = SecurityIdentifier.require(actorId, "Actor ID");
        scopes = Set.copyOf(scopes);
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("At least one scope is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiry is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("Idempotency key is invalid");
        }
        if (auditContext == null) {
            throw new IllegalArgumentException("Audit context is required");
        }
    }
}

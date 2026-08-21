package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;

public record AuditContext(
        String requestId,
        String actorId,
        Ids.AccessKeyId actorKeyId,
        Instant occurredAt
) {
    public AuditContext {
        requestId = SecurityIdentifier.require(requestId, "Request ID");
        actorId = SecurityIdentifier.require(actorId, "Actor ID");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static AuditContext system(String requestId, Instant occurredAt) {
        return new AuditContext(requestId, "system", null, occurredAt);
    }
}

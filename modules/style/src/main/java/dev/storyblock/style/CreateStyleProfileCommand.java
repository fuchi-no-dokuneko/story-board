package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.security.AuditContext;
import java.util.Map;
import java.util.Objects;

public record CreateStyleProfileCommand(
        StyleProfile profile,
        String idempotencyKey,
        String requestHash,
        AuditContext auditContext
) {
    public CreateStyleProfileCommand {
        Objects.requireNonNull(profile, "profile");
        validateKey(idempotencyKey);
        Objects.requireNonNull(auditContext, "auditContext");
        if (!profile.createdBy().equals(auditContext.actorId())
                || !profile.createdAt().equals(auditContext.occurredAt())) {
            throw new IllegalArgumentException(
                    "Style profile creator and time must match audit context"
            );
        }
        String calculated = hash(profile.name(), profile.scope(), profile.provenance());
        if (!calculated.equals(requestHash)) {
            throw new IllegalArgumentException("Style profile request hash does not match payload");
        }
    }

    public static String hash(String name, StyleProfileScope scope, String provenance) {
        return CanonicalJson.hash(Map.of(
                "name", name,
                "provenance", provenance,
                "scope", scope.canonicalValue()
        ));
    }

    static void validateKey(String value) {
        if (value == null || value.isBlank() || value.length() > 200) {
            throw new IllegalArgumentException("Style idempotency key is invalid");
        }
    }
}

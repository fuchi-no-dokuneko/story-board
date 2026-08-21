package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record CreateStyleProfileVersionCommand(
        Ids.StyleProfileId profileId,
        Ids.StyleProfileVersionId versionId,
        Ids.StyleLifecycleEventId initialEventId,
        StyleProfileVersionContent content,
        String expectedProfileHash,
        String idempotencyKey,
        String requestHash,
        AuditContext auditContext
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public CreateStyleProfileVersionCommand {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(versionId, "versionId");
        Objects.requireNonNull(initialEventId, "initialEventId");
        Objects.requireNonNull(content, "content");
        validateHash(expectedProfileHash, "expected profile hash");
        CreateStyleProfileCommand.validateKey(idempotencyKey);
        Objects.requireNonNull(auditContext, "auditContext");
        String calculated = hash(profileId, content, expectedProfileHash);
        if (!calculated.equals(requestHash)) {
            throw new IllegalArgumentException(
                    "Style profile version request hash does not match payload"
            );
        }
    }

    public static String hash(
            Ids.StyleProfileId profileId,
            StyleProfileVersionContent content,
            String expectedProfileHash
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("content", content.canonicalValue());
        value.put("expected_profile_hash", expectedProfileHash);
        value.put("profile_id", profileId.value());
        return CanonicalJson.hash(value);
    }

    static void validateHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Style " + field + " is invalid");
        }
    }
}

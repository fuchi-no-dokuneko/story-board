package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record TransitionStyleProfileVersionCommand(
        Ids.StyleProfileId profileId,
        Ids.StyleProfileVersionId versionId,
        Ids.StyleLifecycleEventId eventId,
        StyleProfileState targetState,
        String reason,
        boolean confirmGeneratedCorpusPromotion,
        String expectedStatusHash,
        String idempotencyKey,
        String requestHash,
        AuditContext auditContext
) {
    public TransitionStyleProfileVersionCommand {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(versionId, "versionId");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(targetState, "targetState");
        if (targetState == StyleProfileState.DRAFT) {
            throw new IllegalArgumentException("Cannot transition an existing version to DRAFT");
        }
        if (reason == null || reason.isBlank() || reason.length() > 1_000) {
            throw new IllegalArgumentException("Style lifecycle reason is invalid");
        }
        CreateStyleProfileVersionCommand.validateHash(
                expectedStatusHash, "expected status hash"
        );
        CreateStyleProfileCommand.validateKey(idempotencyKey);
        Objects.requireNonNull(auditContext, "auditContext");
        String calculated = hash(
                profileId,
                versionId,
                targetState,
                reason,
                confirmGeneratedCorpusPromotion,
                expectedStatusHash
        );
        if (!calculated.equals(requestHash)) {
            throw new IllegalArgumentException(
                    "Style lifecycle request hash does not match payload"
            );
        }
    }

    public static String hash(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            StyleProfileState targetState,
            String reason,
            boolean confirmGeneratedCorpusPromotion,
            String expectedStatusHash
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("confirm_generated_corpus_promotion", confirmGeneratedCorpusPromotion);
        value.put("expected_status_hash", expectedStatusHash);
        value.put("profile_id", profileId.value());
        value.put("reason", reason);
        value.put("target_state", targetState.canonicalName());
        value.put("version_id", versionId.value());
        return CanonicalJson.hash(value);
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import dev.storyblock.style.TransitionStyleProfileVersionCommand;
import java.util.Objects;

final class StyleProfileServiceTransitionAction {
    static StyleProfileVersionSaveResult transition(StyleProfileService self, Ids.StyleProfileId profileId, Ids.StyleProfileVersionId versionId, StyleProfileState targetState, String reason, boolean confirmGeneratedCorpusPromotion, String expectedStatusHash, String idempotencyKey, AuditContext auditContext)  {
        Objects.requireNonNull(auditContext, "auditContext");
        String requestHash = TransitionStyleProfileVersionCommand.hash(
                profileId,
                versionId,
                targetState,
                reason,
                confirmGeneratedCorpusPromotion,
                expectedStatusHash
        );
        return self.profiles.transitionStyleProfileVersion(
                new TransitionStyleProfileVersionCommand(
                        profileId,
                        versionId,
                        Ids.StyleLifecycleEventId.create(),
                        targetState,
                        reason,
                        confirmGeneratedCorpusPromotion,
                        expectedStatusHash,
                        idempotencyKey,
                        requestHash,
                        auditContext
                )
        );
    }
}

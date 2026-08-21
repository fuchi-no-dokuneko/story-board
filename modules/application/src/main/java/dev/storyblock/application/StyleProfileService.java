package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.CreateStyleProfileCommand;
import dev.storyblock.style.CreateStyleProfileVersionCommand;
import dev.storyblock.style.StyleLifecycleConflictException;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileSaveResult;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileStore;
import dev.storyblock.style.StyleProfileVersionContent;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import dev.storyblock.style.StyleProfileVersionView;
import dev.storyblock.style.StyleProfileScope;
import dev.storyblock.style.TransitionStyleProfileVersionCommand;
import java.util.Objects;

public final class StyleProfileService {
    private final StyleProfileStore profiles;

    public StyleProfileService(StyleProfileStore profiles) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
    }

    public StyleProfileSaveResult createProfile(
            String name,
            StyleProfileScope scope,
            String provenance,
            String idempotencyKey,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(auditContext, "auditContext");
        StyleProfile profile = new StyleProfile(
                Ids.StyleProfileId.create(),
                name,
                scope,
                provenance,
                auditContext.actorId(),
                auditContext.occurredAt()
        );
        String requestHash = CreateStyleProfileCommand.hash(name, scope, provenance);
        return profiles.createStyleProfile(new CreateStyleProfileCommand(
                profile, idempotencyKey, requestHash, auditContext
        ));
    }

    public StyleProfile getProfile(Ids.StyleProfileId profileId) {
        return profiles.getStyleProfile(profileId);
    }

    public StyleProfileVersionSaveResult createVersion(
            Ids.StyleProfileId profileId,
            StyleProfileVersionContent content,
            String expectedProfileHash,
            String idempotencyKey,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(auditContext, "auditContext");
        String requestHash = CreateStyleProfileVersionCommand.hash(
                profileId, content, expectedProfileHash
        );
        return profiles.createStyleProfileVersion(new CreateStyleProfileVersionCommand(
                profileId,
                Ids.StyleProfileVersionId.create(),
                Ids.StyleLifecycleEventId.create(),
                content,
                expectedProfileHash,
                idempotencyKey,
                requestHash,
                auditContext
        ));
    }

    public StyleProfileVersionView getVersion(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) {
        return profiles.getStyleProfileVersion(profileId, versionId);
    }

    public StyleProfileVersionSaveResult transition(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            StyleProfileState targetState,
            String reason,
            boolean confirmGeneratedCorpusPromotion,
            String expectedStatusHash,
            String idempotencyKey,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(auditContext, "auditContext");
        String requestHash = TransitionStyleProfileVersionCommand.hash(
                profileId,
                versionId,
                targetState,
                reason,
                confirmGeneratedCorpusPromotion,
                expectedStatusHash
        );
        return profiles.transitionStyleProfileVersion(
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

    public StyleProfileVersionView requireRewriteGate(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) {
        StyleProfileVersionView view = getVersion(profileId, versionId);
        if (!view.canGateRewrites()) {
            throw new StyleLifecycleConflictException(
                    "Only an approved READY style profile version can gate rewrites"
            );
        }
        return view;
    }
}

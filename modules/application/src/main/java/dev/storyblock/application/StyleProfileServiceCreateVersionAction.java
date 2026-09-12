package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.CreateStyleProfileVersionCommand;
import dev.storyblock.style.StyleProfileVersionContent;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import java.util.Objects;

final class StyleProfileServiceCreateVersionAction {
    static StyleProfileVersionSaveResult createVersion(StyleProfileService self, Ids.StyleProfileId profileId, StyleProfileVersionContent content, String expectedProfileHash, String idempotencyKey, AuditContext auditContext)  {
        Objects.requireNonNull(auditContext, "auditContext");
        String requestHash = CreateStyleProfileVersionCommand.hash(
                profileId, content, expectedProfileHash
        );
        return self.profiles.createStyleProfileVersion(new CreateStyleProfileVersionCommand(
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
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.CreateStyleProfileCommand;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileSaveResult;
import dev.storyblock.style.StyleProfileScope;
import java.util.Objects;

final class StyleProfileServiceCreateProfileAction {
    static StyleProfileSaveResult createProfile(StyleProfileService self, String name, StyleProfileScope scope, String provenance, String idempotencyKey, AuditContext auditContext)  {
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
        return self.profiles.createStyleProfile(new CreateStyleProfileCommand(
                profile, idempotencyKey, requestHash, auditContext
        ));
    }
}

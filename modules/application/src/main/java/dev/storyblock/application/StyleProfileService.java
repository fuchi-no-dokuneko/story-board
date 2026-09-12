package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.*;
import java.util.Objects;

public final class StyleProfileService {
  final StyleProfileStore profiles;

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
    return StyleProfileServiceCreateProfileAction.createProfile(this, name, scope, provenance, idempotencyKey, auditContext);
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
    return StyleProfileServiceCreateVersionAction.createVersion(this, profileId, content, expectedProfileHash, idempotencyKey, auditContext);
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
    return StyleProfileServiceTransitionAction.transition(this, profileId, versionId, targetState, reason, confirmGeneratedCorpusPromotion, expectedStatusHash, idempotencyKey, auditContext);
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

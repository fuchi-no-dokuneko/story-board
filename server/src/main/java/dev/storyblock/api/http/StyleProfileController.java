package dev.storyblock.api.http;

import dev.storyblock.application.StyleProfileService;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleProfile;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/style-profiles")
public final class StyleProfileController {
  static final Set<String> PROFILE_FIELDS = Set.of(
      "name", "scope", "provenance"
  );
  static final Set<String> TRANSITION_FIELDS = Set.of(
      "target_state", "reason", "confirm_generated_corpus_promotion"
  );

  final StyleProfileService profiles;
  final Clock clock;

  public StyleProfileController(StyleProfileService profiles, Clock clock) {
    this.profiles = java.util.Objects.requireNonNull(profiles, "profiles");
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
  }

  @PostMapping
  ResponseEntity<Map<String, Object>> createProfile(
      @RequestBody byte[] requestBytes,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication,
      HttpServletRequest servletRequest
  ) {
    return StyleProfileControllerCreateProfileAction.createProfile(this, requestBytes, ifMatch, idempotencyKey, authentication, servletRequest);
  }

  @GetMapping("/{profileId}")
  ResponseEntity<Map<String, Object>> getProfile(
      @PathVariable String profileId,
      Authentication authentication
  ) {
    StyleProfile profile = profiles.getProfile(new Ids.StyleProfileId(profileId));
    AccessPrincipalSupport.requireNovel(authentication, profile.scope().novelId());
    return ResponseEntity.ok()
        .eTag(profile.resourceHash())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(profile.canonicalValue());
  }

  @PostMapping("/{profileId}/versions")
  ResponseEntity<Map<String, Object>> createVersion(
      @PathVariable String profileId,
      @RequestBody byte[] requestBytes,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication,
      HttpServletRequest servletRequest
  ) {
    return StyleProfileControllerCreateVersionAction.createVersion(this, profileId, requestBytes, ifMatch, idempotencyKey, authentication, servletRequest);
  }

  @GetMapping("/{profileId}/versions/{versionId}")
  ResponseEntity<Map<String, Object>> getVersion(
      @PathVariable String profileId,
      @PathVariable String versionId,
      Authentication authentication
  ) {
    return StyleProfileControllerGetVersionAction.getVersion(this, profileId, versionId, authentication);
  }

  @PostMapping("/{profileId}/versions/{versionId}/transitions")
  ResponseEntity<Map<String, Object>> transition(
      @PathVariable String profileId,
      @PathVariable String versionId,
      @RequestBody byte[] requestBytes,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication,
      HttpServletRequest servletRequest
  ) {
    return StyleProfileControllerTransitionAction.transition(this, profileId, versionId, requestBytes, ifMatch, idempotencyKey, authentication, servletRequest);
  }

}

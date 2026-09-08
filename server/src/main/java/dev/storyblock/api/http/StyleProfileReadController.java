package dev.storyblock.api.http;

import dev.storyblock.application.StyleProfileService;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleProfile;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/style-profiles")
final class StyleProfileReadController {
  private final StyleProfileService profiles;
  StyleProfileReadController(StyleProfileService profiles) { this.profiles = profiles; }
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

}

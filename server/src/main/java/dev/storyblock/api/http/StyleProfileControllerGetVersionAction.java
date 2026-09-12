package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class StyleProfileControllerGetVersionAction {
    static ResponseEntity<Map<String, Object>> getVersion(StyleProfileController self, String profileId, String versionId, Authentication authentication)  {
        Ids.StyleProfileId requestedProfile = new Ids.StyleProfileId(profileId);
        StyleProfile profile = self.profiles.getProfile(requestedProfile);
        AccessPrincipalSupport.requireNovel(authentication, profile.scope().novelId());
        StyleProfileVersionView view = self.profiles.getVersion(
                requestedProfile, new Ids.StyleProfileVersionId(versionId)
        );
        return ResponseEntity.ok()
                .eTag(view.statusHash())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(view.canonicalValue());
    }
}

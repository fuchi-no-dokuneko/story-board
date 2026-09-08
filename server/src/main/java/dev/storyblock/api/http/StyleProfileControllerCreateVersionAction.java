package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileVersionContent;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class StyleProfileControllerCreateVersionAction {
    static ResponseEntity<Map<String, Object>> createVersion(StyleProfileController self, String profileId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
        Ids.StyleProfileId requestedProfile = new Ids.StyleProfileId(profileId);
        StyleProfile profile = self.profiles.getProfile(requestedProfile);
        AccessPrincipalSupport.requireNovel(authentication, profile.scope().novelId());
        StyleProfileVersionContent content = StyleProfileVersionContent.fromCanonical(
                StrictJsonRequest.parseObject(
                        requestBytes, "style profile version request"
                )
        );
        Instant now = self.clock.instant();
        StyleProfileVersionSaveResult result = self.profiles.createVersion(
                requestedProfile,
                content,
                StrictJsonRequest.unquoteEtag(ifMatch),
                idempotencyKey,
                AccessPrincipalSupport.auditContext(authentication, servletRequest, now)
        );
        HttpStatus status = result.idempotentReplay()
                ? HttpStatus.OK : HttpStatus.CREATED;
        String location = StyleProfileControllerVersionLocation.versionLocation(result.view());
        return ResponseEntity.status(status)
                .location(URI.create(location))
                .eTag(result.view().statusHash())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.view().canonicalValue());
    }
}

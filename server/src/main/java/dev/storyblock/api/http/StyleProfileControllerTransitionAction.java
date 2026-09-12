package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class StyleProfileControllerTransitionAction {
    static ResponseEntity<Map<String, Object>> transition(StyleProfileController self, String profileId, String versionId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
        Ids.StyleProfileId requestedProfile = new Ids.StyleProfileId(profileId);
        StyleProfile profile = self.profiles.getProfile(requestedProfile);
        AccessPrincipalSupport.requireNovel(authentication, profile.scope().novelId());
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style lifecycle transition"
        );
        StrictJsonRequest.requireKeys(
                request, StyleProfileController.TRANSITION_FIELDS, "style lifecycle transition"
        );
        Object confirmation = request.get("confirm_generated_corpus_promotion");
        if (!(confirmation instanceof Boolean confirmed)) {
            throw new IllegalArgumentException(
                    "style lifecycle transition confirmation must be boolean"
            );
        }
        Instant now = self.clock.instant();
        StyleProfileVersionSaveResult result = self.profiles.transition(
                requestedProfile,
                new Ids.StyleProfileVersionId(versionId),
                StyleProfileState.fromCanonicalName(StrictJsonRequest.string(
                        request, "target_state", "style lifecycle transition"
                )),
                StrictJsonRequest.string(
                        request, "reason", "style lifecycle transition"
                ),
                confirmed,
                StrictJsonRequest.unquoteEtag(ifMatch),
                idempotencyKey,
                AccessPrincipalSupport.auditContext(authentication, servletRequest, now)
        );
        return ResponseEntity.ok()
                .location(URI.create(StyleProfileControllerVersionLocation.versionLocation(result.view())))
                .eTag(result.view().statusHash())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.view().canonicalValue());
    }
}

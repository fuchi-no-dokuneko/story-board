package dev.storyblock.api.http;

import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleProfileScope;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class StyleProfileControllerCreateProfileAction {
    static ResponseEntity<Map<String, Object>> createProfile(StyleProfileController self, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
        if (!"*".equals(ifMatch)) {
            throw new IllegalArgumentException(
                    "Style profile collection creation requires If-Match: *"
            );
        }
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style profile request"
        );
        StrictJsonRequest.requireKeys(
                request, StyleProfileController.PROFILE_FIELDS, "style profile request"
        );
        StyleProfileScope scope = StyleProfileScope.fromCanonical(
                StrictJsonRequest.object(
                        request.get("scope"), "style profile request.scope"
                )
        );
        AccessPrincipalSupport.requireNovel(authentication, scope.novelId());
        Instant now = self.clock.instant();
        AuditContext audit = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        var result = self.profiles.createProfile(
                StrictJsonRequest.string(request, "name", "style profile request"),
                scope,
                StrictJsonRequest.string(
                        request, "provenance", "style profile request"
                ),
                idempotencyKey,
                audit
        );
        HttpStatus status = result.idempotentReplay()
                ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .location(URI.create(
                        "/v1/style-profiles/" + result.profile().profileId().value()
                ))
                .eTag(result.profile().resourceHash())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.profile().canonicalValue());
    }
}

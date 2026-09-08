package dev.storyblock.api.http;

import dev.storyblock.application.StyleProfileService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileScope;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileVersionContent;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import dev.storyblock.style.StyleProfileVersionView;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/style-profiles")
public final class StyleProfileController {
    private static final Set<String> PROFILE_FIELDS = Set.of(
            "name", "scope", "provenance"
    );
    private static final Set<String> TRANSITION_FIELDS = Set.of(
            "target_state", "reason", "confirm_generated_corpus_promotion"
    );

    private final StyleProfileService profiles;
    private final Clock clock;

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
        if (!"*".equals(ifMatch)) {
            throw new IllegalArgumentException(
                    "Style profile collection creation requires If-Match: *"
            );
        }
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style profile request"
        );
        StrictJsonRequest.requireKeys(
                request, PROFILE_FIELDS, "style profile request"
        );
        StyleProfileScope scope = StyleProfileScope.fromCanonical(
                StrictJsonRequest.object(
                        request.get("scope"), "style profile request.scope"
                )
        );
        AccessPrincipalSupport.requireNovel(authentication, scope.novelId());
        Instant now = clock.instant();
        AuditContext audit = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        var result = profiles.createProfile(
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
        Ids.StyleProfileId requestedProfile = new Ids.StyleProfileId(profileId);
        StyleProfile profile = profiles.getProfile(requestedProfile);
        AccessPrincipalSupport.requireNovel(authentication, profile.scope().novelId());
        StyleProfileVersionContent content = StyleProfileVersionContent.fromCanonical(
                StrictJsonRequest.parseObject(
                        requestBytes, "style profile version request"
                )
        );
        Instant now = clock.instant();
        StyleProfileVersionSaveResult result = profiles.createVersion(
                requestedProfile,
                content,
                StrictJsonRequest.unquoteEtag(ifMatch),
                idempotencyKey,
                AccessPrincipalSupport.auditContext(authentication, servletRequest, now)
        );
        HttpStatus status = result.idempotentReplay()
                ? HttpStatus.OK : HttpStatus.CREATED;
        String location = versionLocation(result.view());
        return ResponseEntity.status(status)
                .location(URI.create(location))
                .eTag(result.view().statusHash())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.view().canonicalValue());
    }

    @GetMapping("/{profileId}/versions/{versionId}")
    ResponseEntity<Map<String, Object>> getVersion(
            @PathVariable String profileId,
            @PathVariable String versionId,
            Authentication authentication
    ) {
        Ids.StyleProfileId requestedProfile = new Ids.StyleProfileId(profileId);
        StyleProfile profile = profiles.getProfile(requestedProfile);
        AccessPrincipalSupport.requireNovel(authentication, profile.scope().novelId());
        StyleProfileVersionView view = profiles.getVersion(
                requestedProfile, new Ids.StyleProfileVersionId(versionId)
        );
        return ResponseEntity.ok()
                .eTag(view.statusHash())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(view.canonicalValue());
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
        Ids.StyleProfileId requestedProfile = new Ids.StyleProfileId(profileId);
        StyleProfile profile = profiles.getProfile(requestedProfile);
        AccessPrincipalSupport.requireNovel(authentication, profile.scope().novelId());
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style lifecycle transition"
        );
        StrictJsonRequest.requireKeys(
                request, TRANSITION_FIELDS, "style lifecycle transition"
        );
        Object confirmation = request.get("confirm_generated_corpus_promotion");
        if (!(confirmation instanceof Boolean confirmed)) {
            throw new IllegalArgumentException(
                    "style lifecycle transition confirmation must be boolean"
            );
        }
        Instant now = clock.instant();
        StyleProfileVersionSaveResult result = profiles.transition(
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
                .location(URI.create(versionLocation(result.view())))
                .eTag(result.view().statusHash())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.view().canonicalValue());
    }

    private static String versionLocation(StyleProfileVersionView view) {
        return "/v1/style-profiles/"
                + view.profileVersion().profileId().value()
                + "/versions/"
                + view.profileVersion().versionId().value();
    }
}

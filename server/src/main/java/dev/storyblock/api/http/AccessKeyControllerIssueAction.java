package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessScope;
import dev.storyblock.security.IssueAccessKeyCommand;
import dev.storyblock.security.IssuedAccessKey;
import dev.storyblock.security.StoredAccessKey;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class AccessKeyControllerIssueAction {
    static ResponseEntity<Map<String, Object>> issue(AccessKeyController self, String novelId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        self.requireCurrentHead(requestedNovel, ifMatch);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "access-key request"
        );
        StrictJsonRequest.requireKeys(
                request, Set.of("actor_id", "scopes", "expires_at"),
                "access-key request"
        );
        Set<AccessScope> scopes = new LinkedHashSet<>();
        for (String scope : StrictJsonRequest.uniqueStrings(
                request, "scopes", "access-key request"
        )) {
            scopes.add(AccessScope.fromCanonicalName(scope));
        }
        Instant expiresAt = StrictJsonRequest.instant(
                request, "expires_at", "access-key request"
        );
        AccessPrincipalSupport.requireDelegableAccess(
                authentication, scopes, expiresAt
        );
        Instant now = Instant.now(self.clock);
        IssuedAccessKey issued = self.accessKeys.issue(new IssueAccessKeyCommand(
                requestedNovel,
                StrictJsonRequest.string(request, "actor_id", "access-key request"),
                scopes,
                expiresAt,
                idempotencyKey,
                AccessPrincipalSupport.auditContext(authentication, servletRequest, now)
        ));
        StoredAccessKey key = issued.key();
        return ResponseEntity.created(URI.create("/v1/access-keys/" + key.keyId().value()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(Map.of(
                        "key_id", key.keyId().value(),
                        "novel_id", key.novelId().value(),
                        "actor_id", key.actorId(),
                        "scopes", AccessScope.canonicalNames(key.scopes()),
                        "expires_at", key.expiresAt().toString(),
                        "secret", issued.bearerToken()
                ));
    }
}

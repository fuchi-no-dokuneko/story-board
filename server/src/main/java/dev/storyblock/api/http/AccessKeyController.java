package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyService;
import dev.storyblock.security.AccessScope;
import dev.storyblock.security.IssueAccessKeyCommand;
import dev.storyblock.security.IssuedAccessKey;
import dev.storyblock.security.StoredAccessKey;
import dev.storyblock.storage.RevisionRef;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public final class AccessKeyController {
    private final AccessKeyService accessKeys;
    private final CanonicalTransferService transfers;
    private final Clock clock;

    public AccessKeyController(
            AccessKeyService accessKeys,
            CanonicalTransferService transfers,
            Clock clock
    ) {
        this.accessKeys = java.util.Objects.requireNonNull(accessKeys, "accessKeys");
        this.transfers = java.util.Objects.requireNonNull(transfers, "transfers");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping("/novels/{novelId}/access-keys")
    ResponseEntity<Map<String, Object>> issue(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        requireCurrentHead(requestedNovel, ifMatch);
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
        Instant now = Instant.now(clock);
        IssuedAccessKey issued = accessKeys.issue(new IssueAccessKeyCommand(
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

    @DeleteMapping("/access-keys/{keyId}")
    ResponseEntity<Map<String, Object>> revoke(
            @PathVariable String keyId,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Ids.AccessKeyId requestedKeyId = new Ids.AccessKeyId(keyId);
        StoredAccessKey key = accessKeys.requireKey(requestedKeyId);
        AccessPrincipalSupport.requireNovel(authentication, key.novelId());
        requireCurrentHead(key.novelId(), ifMatch);
        Instant now = Instant.now(clock);
        accessKeys.revoke(
                requestedKeyId,
                key.novelId(),
                AccessPrincipalSupport.auditContext(authentication, servletRequest, now)
        );
        return ResponseEntity.ok(Map.of(
                "key_id", requestedKeyId.value(),
                "revoked", true
        ));
    }

    private void requireCurrentHead(Ids.NovelId novelId, String ifMatch) {
        RevisionRef actual = transfers.getHead(novelId);
        String expectedHash = StrictJsonRequest.unquoteEtag(ifMatch);
        if (!actual.contentHash().equals(expectedHash)) {
            throw new dev.storyblock.storage.StaleHeadException(
                    new RevisionRef(actual.revisionId(), actual.sequence(), expectedHash),
                    actual
            );
        }
    }
}

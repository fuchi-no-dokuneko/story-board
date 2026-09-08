package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyService;
import dev.storyblock.storage.RevisionRef;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
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
    final AccessKeyService accessKeys;
    final CanonicalTransferService transfers;
    final Clock clock;

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
        return AccessKeyControllerIssueAction.issue(this, novelId, requestBytes, ifMatch, idempotencyKey, authentication, servletRequest);
    }

    @DeleteMapping("/access-keys/{keyId}")
    ResponseEntity<Map<String, Object>> revoke(
            @PathVariable String keyId,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return AccessKeyControllerRevokeAction.revoke(this, keyId, ifMatch, authentication, servletRequest);
    }

    void requireCurrentHead(Ids.NovelId novelId, String ifMatch) {
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

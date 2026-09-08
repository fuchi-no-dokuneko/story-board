package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisLease;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class StyleAnalysisControllerClaimAction {
    static ResponseEntity<Map<String, Object>> claim(StyleAnalysisController self, byte[] requestBytes, String idempotencyKey, Authentication authentication)  {
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style job claim"
        );
        StrictJsonRequest.requireKeys(request, StyleAnalysisController.CLAIM_FIELDS, "style job claim");
        Ids.NovelId novelId = new Ids.NovelId(StrictJsonRequest.string(
                request, "novel_id", "style job claim"
        ));
        AccessPrincipalSupport.requireNovel(authentication, novelId);
        Optional<StyleAnalysisLease> lease = self.analyses.claim(
                novelId,
                StrictJsonRequest.string(request, "lease_owner", "style job claim"),
                Duration.ofSeconds(StrictJsonRequest.integer(
                        request, "lease_seconds", "style job claim"
                )),
                idempotencyKey,
                Instant.now(self.clock)
        );
        if (lease.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .eTag(lease.get().claimedStatusHash())
                .body(lease.get().canonicalValue());
    }
}

package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.StoredAccessKey;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class AccessKeyControllerRevokeAction {
    static ResponseEntity<Map<String, Object>> revoke(AccessKeyController self, String keyId, String ifMatch, Authentication authentication, HttpServletRequest servletRequest)  {
        Ids.AccessKeyId requestedKeyId = new Ids.AccessKeyId(keyId);
        StoredAccessKey key = self.accessKeys.requireKey(requestedKeyId);
        AccessPrincipalSupport.requireNovel(authentication, key.novelId());
        self.requireCurrentHead(key.novelId(), ifMatch);
        Instant now = Instant.now(self.clock);
        self.accessKeys.revoke(
                requestedKeyId,
                key.novelId(),
                AccessPrincipalSupport.auditContext(authentication, servletRequest, now)
        );
        return ResponseEntity.ok(Map.of(
                "key_id", requestedKeyId.value(),
                "revoked", true
        ));
    }
}

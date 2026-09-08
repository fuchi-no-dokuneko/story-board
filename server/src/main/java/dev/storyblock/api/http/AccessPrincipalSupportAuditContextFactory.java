package dev.storyblock.api.http;

import dev.storyblock.security.AccessPrincipal;
import dev.storyblock.security.AuditContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.core.Authentication;

final class AccessPrincipalSupportAuditContextFactory {
    static AuditContext auditContext(Authentication authentication, HttpServletRequest request, Instant occurredAt)  {
        Optional<AccessPrincipal> principal = AccessPrincipalSupport.principal(authentication);
        if (principal.isPresent()) {
            AccessPrincipal value = principal.get();
            return new AuditContext(
                    ApiRequestMetadata.requestId(request),
                    value.actorId(),
                    value.keyId(),
                    occurredAt
            );
        }
        String actor = authentication == null ? "authenticated" : authentication.getName();
        if (actor == null || !actor.matches("[A-Za-z0-9._:@-]{1,128}")) {
            actor = "authenticated";
        }
        return new AuditContext(
                ApiRequestMetadata.requestId(request), actor, null, occurredAt
        );
    }
}

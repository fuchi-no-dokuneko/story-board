package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessPrincipal;
import dev.storyblock.security.AccessScope;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.CrossNovelAccessException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;

final class AccessPrincipalSupport {
    AccessPrincipalSupport() {
    }

    static Optional<AccessPrincipal> principal(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccessPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    static void requireNovel(Authentication authentication, Ids.NovelId novelId) {
        principal(authentication).ifPresent(principal -> {
            if (!principal.canAccess(novelId)) {
                throw new CrossNovelAccessException();
            }
        });
    }

    static void requireDelegableAccess(
            Authentication authentication,
            Set<AccessScope> requestedScopes,
            Instant requestedExpiry
    ) {
        AccessPrincipalSupportRequireDelegableAccessFactory.requireDelegableAccess(authentication, requestedScopes, requestedExpiry);
    }

    static AuditContext auditContext(
            Authentication authentication,
            HttpServletRequest request,
            Instant occurredAt
    ) {
        return AccessPrincipalSupportAuditContextFactory.auditContext(authentication, request, occurredAt);
    }
}

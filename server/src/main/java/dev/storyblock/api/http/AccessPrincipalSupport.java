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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

final class AccessPrincipalSupport {
    private AccessPrincipalSupport() {
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
        principal(authentication).ifPresent(principal -> {
            if (!principal.owner() && !principal.scopes().containsAll(requestedScopes)) {
                throw ApiFailureException.of(
                        HttpStatus.FORBIDDEN,
                        "SCOPE_DELEGATION_DENIED",
                        "Scope delegation denied",
                        "scope-delegation-denied",
                        "A credential may delegate only scopes it already holds."
                );
            }
            if (!principal.owner() && requestedExpiry.isAfter(principal.expiresAt())) {
                throw ApiFailureException.of(
                        HttpStatus.FORBIDDEN,
                        "EXPIRY_DELEGATION_DENIED",
                        "Expiry delegation denied",
                        "expiry-delegation-denied",
                        "A delegated credential cannot outlive its issuer."
                );
            }
        });
    }

    static AuditContext auditContext(
            Authentication authentication,
            HttpServletRequest request,
            Instant occurredAt
    ) {
        Optional<AccessPrincipal> principal = principal(authentication);
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

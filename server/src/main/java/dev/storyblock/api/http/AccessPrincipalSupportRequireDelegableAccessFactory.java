package dev.storyblock.api.http;

import dev.storyblock.security.AccessScope;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

final class AccessPrincipalSupportRequireDelegableAccessFactory {
    static void requireDelegableAccess(Authentication authentication, Set<AccessScope> requestedScopes, Instant requestedExpiry)  {
        AccessPrincipalSupport.principal(authentication).ifPresent(principal -> {
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
}

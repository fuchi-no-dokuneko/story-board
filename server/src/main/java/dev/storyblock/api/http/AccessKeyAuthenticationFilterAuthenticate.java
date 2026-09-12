package dev.storyblock.api.http;

import dev.storyblock.security.AccessPrincipal;
import dev.storyblock.security.AccessScope;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

final class AccessKeyAuthenticationFilterAuthenticate {
    static void authenticate(AccessPrincipal principal) {
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>(
                principal.scopes().stream()
                .map(AccessScope::canonicalName)
                .sorted()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList()
        );
        if (principal.owner()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities)
        );
    }
}

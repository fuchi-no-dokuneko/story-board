package dev.storyblock.api.http;

import dev.storyblock.security.AccessAuthenticationException;
import dev.storyblock.security.AccessPrincipal;
import dev.storyblock.storage.StorageException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

final class AccessKeyAuthenticationFilterDoFilterInternalAction {
    static void doFilterInternal(AccessKeyAuthenticationFilter self, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (self.trustedLan) {
            AccessKeyAuthenticationFilterAuthenticate.authenticate(AccessPrincipal.ownerPrincipal());
            filterChain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(AccessKeyAuthenticationFilter.BEARER_PREFIX)
                || authorization.length() == AccessKeyAuthenticationFilter.BEARER_PREFIX.length()) {
            self.reject(request, response);
            return;
        }
        String token = authorization.substring(AccessKeyAuthenticationFilter.BEARER_PREFIX.length());
        AccessPrincipal principal;
        try {
            principal = self.isOwnerToken(token)
                    ? AccessPrincipal.ownerPrincipal()
                    : self.accessKeys.authenticate(token, Instant.now(self.clock));
        } catch (AccessAuthenticationException | IllegalArgumentException failure) {
            SecurityContextHolder.clearContext();
            self.reject(request, response);
            return;
        } catch (StorageException failure) {
            SecurityContextHolder.clearContext();
            self.problemWriter.write(request, response, ApiFailureException.unavailable(
                    "Credential storage is temporarily unavailable."
            ));
            return;
        }
        AccessKeyAuthenticationFilterAuthenticate.authenticate(principal);
        filterChain.doFilter(request, response);
    }
}

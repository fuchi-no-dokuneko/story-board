package dev.storyblock.api.http;

import dev.storyblock.security.AccessAuthenticationException;
import dev.storyblock.security.AccessKeyService;
import dev.storyblock.security.AccessPrincipal;
import dev.storyblock.security.AccessScope;
import dev.storyblock.storage.StorageException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class AccessKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessKeyService accessKeys;
    private final Clock clock;
    private final byte[] ownerTokenHash;
    private final ApiProblemWriter problemWriter;
    private final StoryBlockTelemetry telemetry;

    AccessKeyAuthenticationFilter(
            AccessKeyService accessKeys,
            Clock clock,
            String ownerToken,
            ApiProblemWriter problemWriter,
            StoryBlockTelemetry telemetry
    ) {
        this.accessKeys = java.util.Objects.requireNonNull(accessKeys, "accessKeys");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.problemWriter = java.util.Objects.requireNonNull(problemWriter, "problemWriter");
        this.telemetry = java.util.Objects.requireNonNull(telemetry, "telemetry");
        this.ownerTokenHash = ownerToken == null || ownerToken.isBlank()
                ? null : sha256(ownerToken);
        if (ownerTokenHash != null && ownerToken.length() < 32) {
            throw new IllegalArgumentException(
                    "storyblock.security.owner-token must contain at least 32 characters"
            );
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/v1/openapi.yaml".equals(path)
                || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX)
                || authorization.length() == BEARER_PREFIX.length()) {
            reject(request, response);
            return;
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        AccessPrincipal principal;
        try {
            principal = isOwnerToken(token)
                    ? AccessPrincipal.ownerPrincipal()
                    : accessKeys.authenticate(token, Instant.now(clock));
        } catch (AccessAuthenticationException | IllegalArgumentException failure) {
            SecurityContextHolder.clearContext();
            reject(request, response);
            return;
        } catch (StorageException failure) {
            SecurityContextHolder.clearContext();
            problemWriter.write(request, response, ApiFailureException.unavailable(
                    "Credential storage is temporarily unavailable."
            ));
            return;
        }
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
        filterChain.doFilter(request, response);
    }

    private boolean isOwnerToken(String token) {
        return ownerTokenHash != null && MessageDigest.isEqual(
                ownerTokenHash, sha256(token)
        );
    }

    private void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        telemetry.recordAuthDenied("invalid");
        problemWriter.write(request, response, ApiFailureException.of(
                HttpStatus.UNAUTHORIZED,
                "INVALID_BEARER_CREDENTIAL",
                "Invalid bearer credential",
                "invalid-bearer-credential",
                "The bearer credential is invalid, expired, or revoked."
        ));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }
}

package dev.storyblock.api.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class RequestRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_IDENTITIES = 10_000;

    private final int requestsPerMinute;
    private final Clock clock;
    private final ApiProblemWriter problemWriter;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    RequestRateLimitFilter(
            int requestsPerMinute,
            Clock clock,
            ApiProblemWriter problemWriter
    ) {
        if (requestsPerMinute < 1 || requestsPerMinute > 100_000) {
            throw new IllegalArgumentException("Rate limit must be between 1 and 100000");
        }
        this.requestsPerMinute = requestsPerMinute;
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.problemWriter = java.util.Objects.requireNonNull(problemWriter, "problemWriter");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/")
                || request.getRequestURI().equals("/v1/openapi.yaml");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        long minute = Instant.now(clock).getEpochSecond() / 60L;
        if (windows.size() >= MAX_IDENTITIES) {
            windows.entrySet().removeIf(entry -> entry.getValue().minute() < minute);
        }
        String identity = authentication.getName();
        Window current = windows.compute(identity, (ignored, prior) ->
                prior == null || prior.minute() != minute
                        ? new Window(minute, 1)
                        : new Window(minute, prior.requests() + 1)
        );
        if (current.requests() > requestsPerMinute) {
            problemWriter.write(request, response, new ApiFailureException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Request rate exceeded",
                    "rate-limit-exceeded",
                    "The authenticated identity exceeded its request limit.",
                    java.util.Map.of("limit_per_minute", requestsPerMinute),
                    60
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Window(long minute, int requests) {
    }
}

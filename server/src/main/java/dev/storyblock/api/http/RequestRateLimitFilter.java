package dev.storyblock.api.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

final class RequestRateLimitFilter extends OncePerRequestFilter {
    static final int MAX_IDENTITIES = 10_000;

    final int requestsPerMinute;
    final Clock clock;
    final ApiProblemWriter problemWriter;
    final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

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
        RequestRateLimitFilterDoFilterInternalAction.doFilterInternal(this, request, response, filterChain);
    }

    record Window(long minute, int requests) {
    }
}

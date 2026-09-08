package dev.storyblock.api.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import static dev.storyblock.api.http.RequestRateLimitFilter.Window;

final class RequestRateLimitFilterDoFilterInternalAction {
    static void doFilterInternal(RequestRateLimitFilter self, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        long minute = Instant.now(self.clock).getEpochSecond() / 60L;
        if (self.windows.size() >= RequestRateLimitFilter.MAX_IDENTITIES) {
            self.windows.entrySet().removeIf(entry -> entry.getValue().minute() < minute);
        }
        String identity = authentication.getName();
        Window current = self.windows.compute(identity, (ignored, prior) ->
                prior == null || prior.minute() != minute
                        ? new Window(minute, 1)
                        : new Window(minute, prior.requests() + 1)
        );
        if (current.requests() > self.requestsPerMinute) {
            self.problemWriter.write(request, response, new ApiFailureException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Request rate exceeded",
                    "rate-limit-exceeded",
                    "The authenticated identity exceeded its request limit.",
                    java.util.Map.of("limit_per_minute", self.requestsPerMinute),
                    60
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }
}

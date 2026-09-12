package dev.storyblock.api.http;

import dev.storyblock.api.runtime.Ipv4Listeners;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
final class ListenerWhitelistFilter extends OncePerRequestFilter {
    private final boolean enabled;
    private final Set<String> addresses;
    private final ApiProblemWriter problems;

    ListenerWhitelistFilter(Environment environment, ApiProblemWriter problems) {
        this.problems = problems;
        enabled = environment.getProperty("enable_whitelist", Boolean.class, false);
        String value = environment.getProperty("whitelist", "");
        addresses = new HashSet<>();
        if (!value.isBlank()) {
            for (String item : value.split(",", -1)) {
                addresses.add(Ipv4Listeners.address(item.strip()).getHostAddress());
            }
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (enabled && !addresses.contains(request.getRemoteAddr())) {
            problems.write(request, response, ApiFailureException.of(HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED", "Access denied", "access-denied",
                    "The configured listener whitelist does not include this address."));
            return;
        }
        chain.doFilter(request, response);
    }
}

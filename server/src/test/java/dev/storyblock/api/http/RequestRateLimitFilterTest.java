package dev.storyblock.api.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestRateLimitFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsOnlyAfterTheAuthenticatedIdentityExceedsItsWindow() throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter(
                2,
                Clock.fixed(Instant.parse("2026-08-23T09:00:00Z"), ZoneOffset.UTC),
                new ApiProblemWriter()
        );
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "agent-a", null, java.util.List.of()
                )
        );

        assertEquals(200, invoke(filter).getStatus());
        assertEquals(200, invoke(filter).getStatus());
        MockHttpServletResponse limited = invoke(filter);
        assertEquals(429, limited.getStatus());
        assertEquals("60", limited.getHeader("Retry-After"));
        assertTrue(limited.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
    }

    private static MockHttpServletResponse invoke(RequestRateLimitFilter filter)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/novels/x");
        request.setRequestURI("/v1/novels/x");
        request.setAttribute(ApiRequestMetadata.REQUEST_ID_ATTRIBUTE, "req_rate_test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

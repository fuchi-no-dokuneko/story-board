package dev.storyblock.api.http;

import dev.storyblock.domain.ShortIdScope;
import dev.storyblock.storage.sqlite.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class ShortIdAllocationFilter extends OncePerRequestFilter {
    private final SqliteShortIds allocator;
    public ShortIdAllocationFilter(SqliteRevisionStore store) { allocator = new SqliteShortIds(store); }
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try (var scope = ShortIdScope.open(allocator)) { chain.doFilter(request, response); }
    }
}

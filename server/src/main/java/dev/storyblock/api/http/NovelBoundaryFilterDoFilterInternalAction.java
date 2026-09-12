package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessPrincipal;
import dev.storyblock.security.MissingAccessKeyException;
import dev.storyblock.storage.MissingArtifactException;
import dev.storyblock.storage.MissingExportJobException;
import dev.storyblock.style.MissingStyleAnalysisException;
import dev.storyblock.style.MissingStyleAnalysisJobException;
import dev.storyblock.storage.StorageException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

final class NovelBoundaryFilterDoFilterInternalAction {
    static void doFilterInternal(NovelBoundaryFilter self, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AccessPrincipal principal)
                || principal.owner()) {
            filterChain.doFilter(request, response);
            return;
        }
        if ("POST".equals(request.getMethod())
                && "/v1/novels".equals(request.getRequestURI())) {
            self.reject(request, response);
            return;
        }
        Ids.NovelId requestedNovel;
        try {
            requestedNovel = self.resolveNovel(request.getRequestURI());
        } catch (MissingExportJobException
                 | MissingArtifactException
                 | MissingAccessKeyException failure) {
            filterChain.doFilter(request, response);
            return;
        } catch (MissingStyleAnalysisJobException
                 | MissingStyleAnalysisException failure) {
            filterChain.doFilter(request, response);
            return;
        } catch (StorageException failure) {
            self.problemWriter.write(request, response, ApiFailureException.unavailable(
                    "Authorization storage is temporarily unavailable."
            ));
            return;
        } catch (IllegalArgumentException failure) {
            filterChain.doFilter(request, response);
            return;
        }
        if (requestedNovel != null && !principal.canAccess(requestedNovel)) {
            self.reject(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}

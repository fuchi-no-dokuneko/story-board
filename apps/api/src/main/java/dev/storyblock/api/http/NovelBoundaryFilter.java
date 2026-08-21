package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyService;
import dev.storyblock.security.AccessPrincipal;
import dev.storyblock.security.MissingAccessKeyException;
import dev.storyblock.storage.MissingArtifactException;
import dev.storyblock.storage.MissingExportJobException;
import dev.storyblock.storage.StorageException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class NovelBoundaryFilter extends OncePerRequestFilter {
    private static final Pattern NOVEL_PATH = Pattern.compile(
            "^/v1/novels/(nov_[0-9a-f-]{36})(?:/.*)?$"
    );
    private static final Pattern JOB_PATH = Pattern.compile(
            "^/v1/jobs/(job_[0-9a-f-]{36})$"
    );
    private static final Pattern ARTIFACT_PATH = Pattern.compile(
            "^/v1/artifacts/(art_[0-9a-f-]{36})$"
    );
    private static final Pattern KEY_PATH = Pattern.compile(
            "^/v1/access-keys/(key_[0-9a-f-]{36})$"
    );

    private final CanonicalTransferService transfers;
    private final AccessKeyService accessKeys;
    private final ApiProblemWriter problemWriter;
    private final boolean hideCrossNovel;

    NovelBoundaryFilter(
            CanonicalTransferService transfers,
            AccessKeyService accessKeys,
            ApiProblemWriter problemWriter,
            boolean hideCrossNovel
    ) {
        this.transfers = java.util.Objects.requireNonNull(transfers, "transfers");
        this.accessKeys = java.util.Objects.requireNonNull(accessKeys, "accessKeys");
        this.problemWriter = java.util.Objects.requireNonNull(problemWriter, "problemWriter");
        this.hideCrossNovel = hideCrossNovel;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AccessPrincipal principal)
                || principal.owner()) {
            filterChain.doFilter(request, response);
            return;
        }
        if ("POST".equals(request.getMethod())
                && "/v1/novels".equals(request.getRequestURI())) {
            reject(request, response);
            return;
        }
        Ids.NovelId requestedNovel;
        try {
            requestedNovel = resolveNovel(request.getRequestURI());
        } catch (MissingExportJobException
                 | MissingArtifactException
                 | MissingAccessKeyException failure) {
            filterChain.doFilter(request, response);
            return;
        } catch (StorageException failure) {
            problemWriter.write(request, response, ApiFailureException.unavailable(
                    "Authorization storage is temporarily unavailable."
            ));
            return;
        } catch (IllegalArgumentException failure) {
            filterChain.doFilter(request, response);
            return;
        }
        if (requestedNovel != null && !principal.canAccess(requestedNovel)) {
            reject(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Ids.NovelId resolveNovel(String path) {
        Matcher novel = NOVEL_PATH.matcher(path);
        if (novel.matches()) {
            return new Ids.NovelId(novel.group(1));
        }
        Matcher job = JOB_PATH.matcher(path);
        if (job.matches()) {
            return transfers.getExportJob(new Ids.JobId(job.group(1))).novelId();
        }
        Matcher artifact = ARTIFACT_PATH.matcher(path);
        if (artifact.matches()) {
            return transfers.getArtifact(new Ids.ArtifactId(artifact.group(1))).novelId();
        }
        Matcher key = KEY_PATH.matcher(path);
        if (key.matches()) {
            return accessKeys.requireKey(new Ids.AccessKeyId(key.group(1))).novelId();
        }
        return null;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (hideCrossNovel) {
            problemWriter.write(request, response, ApiFailureException.of(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "Resource not found",
                    "resource-not-found",
                    "The requested resource does not exist."
            ));
        } else {
            problemWriter.write(request, response, ApiFailureException.of(
                    HttpStatus.FORBIDDEN,
                    "NOVEL_ACCESS_DENIED",
                    "Novel access denied",
                    "novel-access-denied",
                    "The credential cannot access the requested novel."
            ));
        }
    }
}

package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.web.filter.OncePerRequestFilter;

final class NovelBoundaryFilter extends OncePerRequestFilter {
    static final Pattern NOVEL_PATH = Pattern.compile(
            "^/v1/novels/(nov_[A-Za-z0-9]{5})(?:/.*)?$"
    );
    static final Pattern JOB_PATH = Pattern.compile(
            "^/v1/jobs/(job_[0-9a-f-]{36})$"
    );
    static final Pattern ARTIFACT_PATH = Pattern.compile(
            "^/v1/artifacts/(art_[0-9a-f-]{36})$"
    );
    static final Pattern INTERNAL_JOB_RESULT_PATH = Pattern.compile(
            "^/v1/internal/jobs/(job_[0-9a-f-]{36})/results$"
    );
    static final Pattern ANALYSIS_PATH = Pattern.compile(
            "^/v1/style-analyses/(ana_[0-9a-f-]{36})(?:/.*)?$"
    );
    static final Pattern KEY_PATH = Pattern.compile(
            "^/v1/access-keys/(key_[0-9a-f-]{36})$"
    );

    final CanonicalTransferService transfers;
    final StyleAnalysisService analyses;
    final AccessKeyService accessKeys;
    final ApiProblemWriter problemWriter;
    final boolean hideCrossNovel;

    NovelBoundaryFilter(
            CanonicalTransferService transfers,
            StyleAnalysisService analyses,
            AccessKeyService accessKeys,
            ApiProblemWriter problemWriter,
            boolean hideCrossNovel
    ) {
        this.transfers = java.util.Objects.requireNonNull(transfers, "transfers");
        this.analyses = java.util.Objects.requireNonNull(analyses, "analyses");
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
        NovelBoundaryFilterDoFilterInternalAction.doFilterInternal(this, request, response, filterChain);
    }

    Ids.NovelId resolveNovel(String path) {
        return NovelBoundaryFilterResolveNovelAction.resolveNovel(this, path);
    }

    void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        NovelBoundaryFilterRejectAction.reject(this, request, response);
    }
}

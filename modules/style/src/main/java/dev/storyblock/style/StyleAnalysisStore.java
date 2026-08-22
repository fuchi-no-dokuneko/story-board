package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Optional;

public interface StyleAnalysisStore {
    StyleAnalysisJobSaveResult createStyleAnalysisJob(StyleAnalysisJob job);

    StyleAnalysisJob getStyleAnalysisJob(Ids.JobId jobId);

    StyleAnalysisJob getStyleAnalysis(Ids.StyleAnalysisId analysisId);

    Optional<StyleAnalysisLease> claimStyleAnalysis(StyleAnalysisClaimCommand command);

    StyleAnalysisCompletionResult completeStyleAnalysis(
            StyleAnalysisCompletionCommand command
    );

    StyleAnalysisJob failStyleAnalysis(
            Ids.JobId jobId,
            String leaseOwner,
            int attempt,
            String expectedStatusHash,
            String failureCode,
            Instant failedAt
    );

    Optional<StyleAnalysisResult> findStyleAnalysisResult(
            Ids.StyleAnalysisId analysisId
    );

    StyleAnalysisWindowSlice listStyleAnalysisWindows(
            Ids.StyleAnalysisId analysisId,
            int afterOrdinal,
            int limit
    );

    Optional<Instant> findStyleArtifactExpiry(Ids.ArtifactId artifactId);
}

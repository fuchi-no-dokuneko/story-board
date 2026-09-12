package dev.storyblock.application;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

abstract class StyleAnalysisQueries {
  final StyleAnalysisStore analyses;
  StyleAnalysisQueries(StyleAnalysisStore analyses) {
    this.analyses = Objects.requireNonNull(analyses, "analyses");
  }
  public StyleAnalysisJob getJob(Ids.JobId jobId) {
    return analyses.getStyleAnalysisJob(jobId);
  }

  public StyleAnalysisJob getAnalysis(Ids.StyleAnalysisId analysisId) {
    return analyses.getStyleAnalysis(analysisId);
  }

  public StyleAnalysisCompletionResult complete(
      StyleAnalysisCompletionCommand command
  ) {
    return analyses.completeStyleAnalysis(command);
  }

  public StyleAnalysisJob fail(
      Ids.JobId jobId,
      String leaseOwner,
      int attempt,
      String expectedStatusHash,
      String failureCode,
      Instant failedAt
  ) {
    return analyses.failStyleAnalysis(
        jobId,
        leaseOwner,
        attempt,
        expectedStatusHash,
        failureCode,
        failedAt
    );
  }

  public Optional<StyleAnalysisResult> result(Ids.StyleAnalysisId analysisId) {
    return analyses.findStyleAnalysisResult(analysisId);
  }

  public void requireArtifactAvailable(Ids.ArtifactId artifactId, Instant now) {
    analyses.findStyleArtifactExpiry(artifactId).ifPresent(expiry -> {
      if (!now.isBefore(expiry)) {
        throw new ExpiredStyleArtifactException(artifactId);
      }
    });
  }
}

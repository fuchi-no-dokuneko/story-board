package dev.storyblock.storage.sqlite;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.*;
import java.time.Instant;
import java.util.*;
interface SqliteAnalysisPort extends StyleAnalysisStore, SqliteStoreContext {
  @Override
  default StyleAnalysisJobSaveResult createStyleAnalysisJob(StyleAnalysisJob job) {
    Objects.requireNonNull(job, "job");
    return context().write(connection -> SqliteAnalysisCreateJob.createJob(connection, job));
  }
  @Override
  default StyleAnalysisJob getStyleAnalysisJob(Ids.JobId jobId) {
    Objects.requireNonNull(jobId, "jobId");
    return context().read(connection -> SqliteAnalysisGetJob.getJob(connection, jobId));
  }
  @Override
  default StyleAnalysisJob getStyleAnalysis(Ids.StyleAnalysisId analysisId) {
    Objects.requireNonNull(analysisId, "analysisId");
    return context().read(connection -> SqliteAnalysisGetAnalysis.getAnalysis(
        connection, analysisId
    ));
  }
  @Override
  default Optional<StyleAnalysisLease> claimStyleAnalysis(
      StyleAnalysisClaimCommand command
  ) {
    Objects.requireNonNull(command, "command");
    return context().write(connection -> SqliteAnalysisClaim.claim(connection, command));
  }
  @Override
  default StyleAnalysisCompletionResult completeStyleAnalysis(
      StyleAnalysisCompletionCommand command
  ) {
    Objects.requireNonNull(command, "command");
    return context().write(connection -> SqliteAnalysisComplete.complete(
        connection, command
    ));
  }
  @Override
  default StyleAnalysisJob failStyleAnalysis(
      Ids.JobId jobId,
      String leaseOwner,
      int attempt,
      String expectedStatusHash,
      String failureCode,
      Instant failedAt
  ) {
    Objects.requireNonNull(jobId, "jobId");
    Objects.requireNonNull(failedAt, "failedAt");
    return context().write(connection -> SqliteAnalysisFail.fail(
        connection,
        jobId,
        leaseOwner,
        attempt,
        expectedStatusHash,
        failureCode,
        failedAt
    ));
  }
  @Override
  default Optional<StyleAnalysisResult> findStyleAnalysisResult(
      Ids.StyleAnalysisId analysisId
  ) {
    Objects.requireNonNull(analysisId, "analysisId");
    return context().read(connection -> SqliteAnalysisFindResult.findResult(
        connection, analysisId
    ));
  }
  @Override
  default StyleAnalysisWindowSlice listStyleAnalysisWindows(
      Ids.StyleAnalysisId analysisId,
      int afterOrdinal,
      int limit
  ) {
    Objects.requireNonNull(analysisId, "analysisId");
    return context().read(connection -> SqliteAnalysisListWindows.listWindows(
        connection, analysisId, afterOrdinal, limit
    ));
  }
  @Override
  default Optional<Instant> findStyleArtifactExpiry(Ids.ArtifactId artifactId) {
    Objects.requireNonNull(artifactId, "artifactId");
    return context().read(connection -> SqliteAnalysisFindArtifactExpiry.findArtifactExpiry(
        connection, artifactId
    ));
  }
}

package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record StyleAnalysisCompletionCommand(
    Ids.JobId jobId,
    String leaseOwner,
    int attempt,
    String expectedStatusHash,
    String snapshotHash,
    String profileVersionHash,
    String analyzerContractHash,
    String windowConfigurationHash,
    StyleAnalysisSummary summary,
    List<StyleAnalysisWindowFinding> windows,
    StyleAnalysisTrace trace,
    String idempotencyKey,
    Instant completedAt
) {
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public StyleAnalysisCompletionCommand {
    Objects.requireNonNull(jobId, "jobId");
    if (leaseOwner == null || leaseOwner.isBlank() || leaseOwner.length() > 128
        || attempt < 1) {
      throw new IllegalArgumentException("Style completion lease identity is invalid");
    }
    StyleAnalysisCompletionCommandRequireHash.requireHash(expectedStatusHash, "status");
    StyleAnalysisCompletionCommandRequireHash.requireHash(snapshotHash, "snapshot");
    StyleAnalysisCompletionCommandRequireHash.requireHash(profileVersionHash, "profile version");
    StyleAnalysisCompletionCommandRequireHash.requireHash(analyzerContractHash, "analyzer contract");
    StyleAnalysisCompletionCommandRequireHash.requireHash(windowConfigurationHash, "window configuration");
    Objects.requireNonNull(summary, "summary");
    windows = List.copyOf(windows);
    StyleAnalysisCompletionCommandValidation.validate(summary, windows, trace, idempotencyKey, completedAt);
  }

  public String resultHash() {
    return StyleAnalysisCompletionCommandResultHashAction.resultHash(this);
  }

  public String requestHash() {
    return CanonicalJson.hash(Map.of(
        "attempt", attempt,
        "job_id", jobId.value(),
        "lease_owner", leaseOwner,
        "result_hash", resultHash()
    ));
  }

}

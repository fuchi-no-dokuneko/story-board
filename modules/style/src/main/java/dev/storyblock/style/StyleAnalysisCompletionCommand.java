package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
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
    if (windows.size() != summary.operationalWindowCount()
        || windows.size() > StyleAnalysisSnapshot.MAX_BLOCKS
        || new HashSet<>(windows.stream().map(
            StyleAnalysisWindowFinding::windowId
        ).toList()).size() != windows.size()) {
      throw new IllegalArgumentException(
          "Style completion windows do not match the summary"
      );
    }
    EnumMap<StyleDecisionState, Integer> decisions = new EnumMap<>(
        StyleDecisionState.class
    );
    for (int index = 0; index < windows.size(); index++) {
      StyleAnalysisWindowFinding window = windows.get(index);
      if (window.ordinal() != index) {
        throw new IllegalArgumentException(
            "Style completion window ordinals must be contiguous"
        );
      }
      decisions.merge(window.decisionState(), 1, Integer::sum);
    }
    for (StyleDecisionState state : StyleDecisionState.values()) {
      if (summary.decisionCounts().get(state).intValue()
          != decisions.getOrDefault(state, 0).intValue()) {
        throw new IllegalArgumentException(
            "Style completion decision totals do not match windows"
        );
      }
    }
    Objects.requireNonNull(trace, "trace");
    if (idempotencyKey == null || idempotencyKey.isBlank()
        || idempotencyKey.length() > 200) {
      throw new IllegalArgumentException(
          "Style completion idempotency key is invalid"
      );
    }
    Objects.requireNonNull(completedAt, "completedAt");
    if (!trace.createdAt().equals(completedAt)) {
      throw new IllegalArgumentException(
          "Style trace and completion timestamps must match"
      );
    }
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

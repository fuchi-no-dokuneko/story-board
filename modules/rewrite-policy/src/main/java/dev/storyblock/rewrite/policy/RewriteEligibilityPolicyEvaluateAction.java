package dev.storyblock.rewrite.policy;

import dev.storyblock.style.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

final class RewriteEligibilityPolicyEvaluateAction {
  static RewriteEligibility evaluate(RewriteEligibilityPolicy self, StyleAnalysisJob analysis, StyleProfileVersionView currentProfile, List<StyleAnalysisWindowFinding> selectedFindings, Instant evaluatedAt)  {
    Objects.requireNonNull(analysis, "analysis");
    Objects.requireNonNull(currentProfile, "currentProfile");
    Objects.requireNonNull(evaluatedAt, "evaluatedAt");
    if (analysis.status() != StyleAnalysisJobStatus.SUCCEEDED
        || analysis.resultHash() == null
        || !evaluatedAt.isBefore(analysis.retentionUntil())) {
      throw new RewriteEligibilityException(
          "Rewrite requires a completed retained style analysis"
      );
    }
    var snapshotVersion = analysis.snapshot().profileVersion();
    var currentVersion = currentProfile.profileVersion();
    if (!currentProfile.canGateRewrites()
        || !snapshotVersion.profileId().equals(currentVersion.profileId())
        || !snapshotVersion.versionId().equals(currentVersion.versionId())
        || !snapshotVersion.versionHash().equals(currentVersion.versionHash())) {
      throw new RewriteEligibilityException(
          "Rewrite requires the analysis profile version to be approved and READY"
      );
    }

    selectedFindings = new ArrayList<>(List.copyOf(selectedFindings));
    selectedFindings.sort(Comparator.comparingInt(
        StyleAnalysisWindowFinding::ordinal
    ));
    if (selectedFindings.isEmpty()
        || new HashSet<>(selectedFindings.stream().map(
            StyleAnalysisWindowFinding::windowId
        ).toList()).size() != selectedFindings.size()
        || new HashSet<>(selectedFindings.stream().map(
            StyleAnalysisWindowFinding::ordinal
        ).toList()).size() != selectedFindings.size()) {
      throw new RewriteEligibilityException(
          "Rewrite findings must be nonempty and unique"
      );
    }

    RewriteFindingRange range = RewriteFindingRange.evaluate(analysis, selectedFindings);
    return new RewriteEligibility(
        analysis.analysisId(),
        analysis.resultHash(),
        analysis.snapshot().novelId(),
        analysis.snapshot().revisionId(),
        analysis.snapshot().revisionHash(),
        currentVersion.profileId(),
        currentVersion.versionId(),
        currentVersion.versionHash(),
        analysis.snapshot().analyzerContractHash(),
        analysis.snapshot().windowConfigurationHash(),
        selectedFindings.stream().map(
            StyleAnalysisWindowFinding::windowId
        ).toList(),
        range.affected(),
        range.decisions()
    );
  }
}

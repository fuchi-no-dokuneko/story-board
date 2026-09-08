package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.style.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record RewriteEligibility(
    Ids.StyleAnalysisId analysisId,
    String analysisResultHash,
    Ids.NovelId novelId,
    Ids.RevisionId revisionId,
    String revisionHash,
    Ids.StyleProfileId profileId,
    Ids.StyleProfileVersionId profileVersionId,
    String profileVersionHash,
    String analyzerContractHash,
    String windowConfigurationHash,
    List<String> findingIds,
    List<Ids.BlockId> affectedBlockIds,
    List<StyleAnomalyDecision> decisions
) {
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  static final Set<String> FIELDS = Set.of(
      "affected_block_ids", "analysis_id", "analysis_result_hash",
      "analyzer_contract_hash", "decisions", "finding_ids", "novel_id",
      "profile_id", "profile_version_hash", "profile_version_id",
      "revision_hash", "revision_id", "window_configuration_hash"
  );

  public RewriteEligibility {
    Objects.requireNonNull(analysisId, "analysisId");
    RewriteEligibilityRequireHash.requireHash(analysisResultHash, "analysis result");
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(revisionId, "revisionId");
    RewriteEligibilityRequireHash.requireHash(revisionHash, "revision");
    Objects.requireNonNull(profileId, "profileId");
    Objects.requireNonNull(profileVersionId, "profileVersionId");
    RewriteEligibilityRequireHash.requireHash(profileVersionHash, "profile version");
    RewriteEligibilityRequireHash.requireHash(analyzerContractHash, "analyzer contract");
    RewriteEligibilityRequireHash.requireHash(windowConfigurationHash, "window configuration");
    findingIds = List.copyOf(findingIds);
    affectedBlockIds = List.copyOf(affectedBlockIds);
    decisions = List.copyOf(decisions);
    if (findingIds.isEmpty() || findingIds.size() > RewriteModule.MAX_FINDINGS
        || new HashSet<>(findingIds).size() != findingIds.size()
        || findingIds.stream().anyMatch(id -> !HASH.matcher(id).matches())
        || decisions.size() != findingIds.size()) {
      throw new IllegalArgumentException("Rewrite eligibility findings are invalid");
    }
    if (affectedBlockIds.isEmpty()
        || affectedBlockIds.size() > RewriteModule.MAX_EDITABLE_BLOCKS
        || new HashSet<>(affectedBlockIds).size() != affectedBlockIds.size()) {
      throw new IllegalArgumentException(
          "Rewrite eligibility affected blocks are invalid"
      );
    }
    for (int index = 0; index < decisions.size(); index++) {
      StyleAnomalyDecision decision = decisions.get(index);
      if (!findingIds.get(index).equals(decision.operationalWindowId())
          || decision.state() != StyleDecisionState.REWRITE_CANDIDATE
          || decision.reason()
          != StyleDecisionReason.SUSTAINED_MULTI_CHANNEL_Q99
          || decision.confidence()
          != StyleCalibrationConfidence.CALIBRATED
          || !decision.canTriggerRewrite()
          || decision.independentQ99Channels().size() < 2
          || decision.sustainingWindowIds().size() < 2
          || decision.intentionalShiftAdjusted()) {
        throw new IllegalArgumentException(
            "Rewrite eligibility contains an ineligible style decision"
        );
      }
    }
  }

  public static RewriteEligibility fromCanonical(Map<String, Object> value) {
    return RewriteEligibilityFromCanonicalFactory.fromCanonical(value);
  }

  public String eligibilityHash() {
    return CanonicalJson.hash(canonicalValue());
  }

  public Map<String, Object> canonicalValue() {
    return RewriteEligibilityCanonicalValueAction.canonicalValue(this);
  }

}

package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.style.StyleAnomalyDecision;
import dev.storyblock.style.StyleCalibrationConfidence;
import dev.storyblock.style.StyleDecisionReason;
import dev.storyblock.style.StyleDecisionState;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "affected_block_ids", "analysis_id", "analysis_result_hash",
            "analyzer_contract_hash", "decisions", "finding_ids", "novel_id",
            "profile_id", "profile_version_hash", "profile_version_id",
            "revision_hash", "revision_id", "window_configuration_hash"
    );

    public RewriteEligibility {
        Objects.requireNonNull(analysisId, "analysisId");
        requireHash(analysisResultHash, "analysis result");
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        requireHash(revisionHash, "revision");
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(profileVersionId, "profileVersionId");
        requireHash(profileVersionHash, "profile version");
        requireHash(analyzerContractHash, "analyzer contract");
        requireHash(windowConfigurationHash, "window configuration");
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
        RewritePolicyCanonical.requireKeys(value, FIELDS, "rewrite_eligibility");
        return new RewriteEligibility(
                new Ids.StyleAnalysisId(RewritePolicyCanonical.string(
                        value, "analysis_id", "rewrite_eligibility"
                )),
                RewritePolicyCanonical.string(
                        value, "analysis_result_hash", "rewrite_eligibility"
                ),
                new Ids.NovelId(RewritePolicyCanonical.string(
                        value, "novel_id", "rewrite_eligibility"
                )),
                new Ids.RevisionId(RewritePolicyCanonical.string(
                        value, "revision_id", "rewrite_eligibility"
                )),
                RewritePolicyCanonical.string(
                        value, "revision_hash", "rewrite_eligibility"
                ),
                new Ids.StyleProfileId(RewritePolicyCanonical.string(
                        value, "profile_id", "rewrite_eligibility"
                )),
                new Ids.StyleProfileVersionId(RewritePolicyCanonical.string(
                        value, "profile_version_id", "rewrite_eligibility"
                )),
                RewritePolicyCanonical.string(
                        value, "profile_version_hash", "rewrite_eligibility"
                ),
                RewritePolicyCanonical.string(
                        value, "analyzer_contract_hash", "rewrite_eligibility"
                ),
                RewritePolicyCanonical.string(
                        value, "window_configuration_hash", "rewrite_eligibility"
                ),
                RewritePolicyCanonical.strings(
                        value.get("finding_ids"), "rewrite_eligibility.finding_ids"
                ),
                RewritePolicyCanonical.strings(
                        value.get("affected_block_ids"),
                        "rewrite_eligibility.affected_block_ids"
                ).stream().map(Ids.BlockId::new).toList(),
                RewritePolicyCanonical.objects(
                        value.get("decisions"), "rewrite_eligibility.decisions"
                ).stream().map(StyleAnomalyDecision::fromCanonical).toList()
        );
    }

    public String eligibilityHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("affected_block_ids", affectedBlockIds.stream()
                .map(Ids.BlockId::value).toList());
        value.put("analysis_id", analysisId.value());
        value.put("analysis_result_hash", analysisResultHash);
        value.put("analyzer_contract_hash", analyzerContractHash);
        value.put("decisions", decisions.stream()
                .map(StyleAnomalyDecision::canonicalValue).toList());
        value.put("finding_ids", findingIds);
        value.put("novel_id", novelId.value());
        value.put("profile_id", profileId.value());
        value.put("profile_version_hash", profileVersionHash);
        value.put("profile_version_id", profileVersionId.value());
        value.put("revision_hash", revisionHash);
        value.put("revision_id", revisionId.value());
        value.put("window_configuration_hash", windowConfigurationHash);
        return CanonicalValues.freezeMap(value, "rewrite_eligibility");
    }

    private static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Rewrite " + field + " hash is invalid");
        }
    }
}

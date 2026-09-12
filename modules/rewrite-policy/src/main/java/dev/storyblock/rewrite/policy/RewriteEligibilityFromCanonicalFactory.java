package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnomalyDecision;
import java.util.Map;

final class RewriteEligibilityFromCanonicalFactory {
    static RewriteEligibility fromCanonical(Map<String, Object> value)  {
        RewritePolicyCanonical.requireKeys(value, RewriteEligibility.FIELDS, "rewrite_eligibility");
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
}

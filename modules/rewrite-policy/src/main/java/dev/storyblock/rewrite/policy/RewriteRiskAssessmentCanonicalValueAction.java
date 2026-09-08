package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteRiskAssessmentCanonicalValueAction {
    static Map<String, Object> canonicalValue(RewriteRiskAssessment self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("candidate_facts", self.candidateFacts().stream()
                .map(RewriteProtectedFactSnapshot::canonicalValue).toList());
        value.put("fact_differences", self.factDifferences().stream()
                .map(RewriteFactDifference::canonicalValue).toList());
        value.put("manual_risk_reasons", self.manualRiskReasons());
        value.put("near_copy_findings", self.nearCopyFindings().stream()
                .map(RewriteNearCopyFinding::canonicalValue).toList());
        value.put("policy_version", RewritePolicyModule.VERSION);
        value.put("proposal_hash", self.proposalHash());
        value.put("proposal_id", self.proposalId().value());
        value.put("source_facts", self.sourceFacts().stream()
                .map(RewriteProtectedFactSnapshot::canonicalValue).toList());
        value.put("state", self.state().canonicalName());
        return CanonicalValues.freezeMap(value, "rewrite_risk_assessment");
    }
}

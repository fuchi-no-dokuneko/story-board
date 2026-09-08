package dev.storyblock.application;

import java.util.Map;

final class RewriteProposalReviewContractFieldsAction {
    static Map<String, Object> contractFields(RewriteProposalReview self)  {
        Map<String, Object> value = new java.util.LinkedHashMap<>();
        value.put("after_style_score", self.afterStyleScore());
        value.put("before_style_score", self.beforeStyleScore());
        value.put("candidate_created_at", self.candidateCreatedAt() == null
                ? null : self.candidateCreatedAt().toString());
        value.put("candidate_revision_id", self.candidateRevisionId() == null
                ? null : self.candidateRevisionId().value());
        value.put("committable_without_approval", self.committableWithoutApproval());
        value.put("expires_at", self.expiresAt().toString());
        value.put("preview", self.preview() == null ? null : self.preview().contractFields());
        value.put("proposal_hash", self.proposalHash());
        value.put("proposal_id", self.proposalId().value());
        value.put("risk_assessment", self.riskAssessment() == null
                ? null : self.riskAssessment().canonicalValue());
        value.put("stale_reasons", self.staleReasons());
        value.put("state", self.state().canonicalName());
        return java.util.Collections.unmodifiableMap(value);
    }
}

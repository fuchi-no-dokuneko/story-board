package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewriteRiskAssessment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RewriteProposalReview(
        Ids.ProposalId proposalId,
        String proposalHash,
        RewriteReviewState state,
        List<String> staleReasons,
        RewriteRiskAssessment riskAssessment,
        PreviewResponse preview,
        Map<String, Object> beforeStyleScore,
        Map<String, Object> afterStyleScore,
        Ids.RevisionId candidateRevisionId,
        Instant candidateCreatedAt,
        Instant expiresAt
) {
    public RewriteProposalReview {
        Objects.requireNonNull(proposalId, "proposalId");
        Objects.requireNonNull(proposalHash, "proposalHash");
        Objects.requireNonNull(state, "state");
        staleReasons = List.copyOf(staleReasons);
        beforeStyleScore = Map.copyOf(beforeStyleScore);
        afterStyleScore = Map.copyOf(afterStyleScore);
        Objects.requireNonNull(expiresAt, "expiresAt");
        if ((state == RewriteReviewState.STALE) != !staleReasons.isEmpty()) {
            throw new IllegalArgumentException(
                    "Rewrite stale state does not match stale reasons"
            );
        }
        if (state == RewriteReviewState.STALE || state == RewriteReviewState.EXPIRED) {
            if (preview != null || riskAssessment != null
                    || candidateRevisionId != null || candidateCreatedAt != null) {
                throw new IllegalArgumentException(
                        "Stale or expired rewrites cannot carry a committable preview"
                );
            }
        } else if (preview == null || riskAssessment == null
                || candidateRevisionId == null || candidateCreatedAt == null) {
            throw new IllegalArgumentException(
                    "Reviewed rewrite lacks deterministic review output"
            );
        }
    }

    public boolean committableWithoutApproval() {
        return state == RewriteReviewState.READY && preview.committable();
    }

    public Map<String, Object> contractFields() {
        return RewriteProposalReviewContractFieldsAction.contractFields(this);
    }
}

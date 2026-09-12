package dev.storyblock.application;

import dev.storyblock.rewrite.RewriteTextProposal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class RewriteProposalReviewServiceUnavailable {
    static RewriteProposalReview unavailable(
            RewriteTextProposal proposal,
            RewriteReviewState state,
            List<String> staleReasons,
            Instant expiresAt
    ) {
        return new RewriteProposalReview(
                proposal.proposalId(), proposal.proposalHash(), state,
                staleReasons, null, null, Map.of(), Map.of(), null, null, expiresAt
        );
    }
}

package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;

public interface RewriteReservationStore {
    RewriteCandidateReservationSaveResult reserveRewriteCandidate(
            ReserveRewriteCandidateCommand command
    );

    RewriteCandidateReservation getRewriteCandidateReservation(
            Ids.ProposalId proposalId
    );
}

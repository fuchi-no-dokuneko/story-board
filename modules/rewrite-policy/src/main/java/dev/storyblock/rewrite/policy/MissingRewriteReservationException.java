package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;

public final class MissingRewriteReservationException extends RuntimeException {
    private final Ids.ProposalId proposalId;

    public MissingRewriteReservationException(Ids.ProposalId proposalId) {
        super("Rewrite reservation does not exist: " + proposalId.value());
        this.proposalId = proposalId;
    }

    public Ids.ProposalId proposalId() {
        return proposalId;
    }
}

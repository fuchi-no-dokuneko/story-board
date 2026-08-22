package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;

public final class RewriteFindingAlreadyReservedException extends RuntimeException {
    private final String findingId;
    private final Ids.ProposalId existingProposalId;

    public RewriteFindingAlreadyReservedException(
            String findingId,
            Ids.ProposalId existingProposalId
    ) {
        super("Style finding already has a rewrite candidate reservation");
        this.findingId = findingId;
        this.existingProposalId = existingProposalId;
    }

    public String findingId() {
        return findingId;
    }

    public Ids.ProposalId existingProposalId() {
        return existingProposalId;
    }
}

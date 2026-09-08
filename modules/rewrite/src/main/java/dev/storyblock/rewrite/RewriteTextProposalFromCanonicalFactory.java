package dev.storyblock.rewrite;

import java.util.Map;

final class RewriteTextProposalFromCanonicalFactory {
    static RewriteTextProposal fromCanonical(Map<String, Object> value)  {
        RewriteCanonical.requireKeys(value, RewriteTextProposal.FIELDS, "rewrite_text_proposal");
        if (!RewriteModule.PROPOSAL_SCHEMA_VERSION.equals(RewriteCanonical.string(
                value, "schema_version", "rewrite_text_proposal"
        ))) {
            throw new IllegalArgumentException("Rewrite proposal schema is unsupported");
        }
        RewriteWorkerInput input = RewriteWorkerInput.fromCanonical(
                RewriteCanonical.object(value.get("input"), "rewrite_text_proposal.input")
        );
        if (!input.proposalId().value().equals(RewriteCanonical.string(
                value, "proposal_id", "rewrite_text_proposal"
        )) || !input.inputHash().equals(RewriteCanonical.string(
                value, "input_hash", "rewrite_text_proposal"
        ))) {
            throw new IllegalArgumentException("Rewrite proposal input binding is invalid");
        }
        RewriteTextProposal proposal = new RewriteTextProposal(
                input,
                RewriteCanonical.string(value, "model_id", "rewrite_text_proposal"),
                RewriteCanonical.string(
                        value, "model_response_hash", "rewrite_text_proposal"
                ),
                RewriteCanonical.objects(
                        value.get("candidates"), "rewrite_text_proposal.candidates"
                ).stream().map(RewriteCandidateBlock::fromCanonical).toList(),
                RewriteCanonical.instant(
                        value, "created_at", "rewrite_text_proposal"
                )
        );
        if (!proposal.proposalHash().equals(RewriteCanonical.string(
                value, "proposal_hash", "rewrite_text_proposal"
        ))) {
            throw new IllegalArgumentException("Rewrite proposal hash is invalid");
        }
        return proposal;
    }
}

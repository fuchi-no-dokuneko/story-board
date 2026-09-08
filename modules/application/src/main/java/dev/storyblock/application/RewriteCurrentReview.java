package dev.storyblock.application;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Instant;
import java.util.List;
import static dev.storyblock.application.RewriteProposalReviewService.CandidateEdit;
import static dev.storyblock.application.RewriteProposalReviewService.StyleScores;

final class RewriteCurrentReview {
  static RewriteProposalReview evaluate(RewriteProposalReviewService self, RewriteTextProposal proposal, List<RewriteReferenceCorpus> corpora, Instant reviewedAt, Instant expiresAt, RewriteCandidateReservation reservation, StyleAnalysisJob analysis, StyleProfileVersionView profile, StoredRevision stored) {
    List<NarrativeBlock> sourceInput = RewriteProposalReviewServiceExactInputBlocks.exactInputBlocks(
        proposal, analysis.snapshot().blocks().stream()
            .map(value -> value.block()).toList()
    );
    RewriteRiskAssessment risk = self.risks.evaluate(
        proposal,
        sourceInput,
        analysis.snapshot().maskingLexicon(),
        profile,
        corpora
    );
    CandidateEdit edit = RewriteProposalReviewServiceCandidateEdit.candidateEdit(stored, reservation, proposal, reviewedAt);
    PreviewResponse preview = new PreviewService(revisionId -> self.revisions
        .getRevision(reservation.novelId(), revisionId).manifest())
        .preview(
            stored.manifest(),
            edit.operation(),
            edit.candidateRevisionId(),
            reviewedAt
        );
    StyleScores scores = self.scores(
        profile,
        analysis,
        reservation,
        proposal
    );
    RewriteReviewState state = risk.state() == RewriteRiskState.BLOCKED
        || !preview.committable()
        ? RewriteReviewState.REJECTED
        : risk.state() == RewriteRiskState.MANUAL_ONLY
        ? RewriteReviewState.MANUAL_ONLY
        : RewriteReviewState.READY;
    return new RewriteProposalReview(
        proposal.proposalId(),
        proposal.proposalHash(),
        state,
        List.of(),
        risk,
        preview,
        scores.before(),
        scores.after(),
        edit.candidateRevisionId(),
        reviewedAt,
        expiresAt
    );  }
}

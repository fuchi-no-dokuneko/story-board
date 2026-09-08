package dev.storyblock.application;

import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

final class RewriteProposalReviewServiceReviewAction {
  static RewriteProposalReview review(RewriteProposalReviewService self, RewriteTextProposal proposal, List<RewriteReferenceCorpus> corpora, Instant reviewedAt, Duration expiry)  {
    Objects.requireNonNull(proposal, "proposal");
    Objects.requireNonNull(reviewedAt, "reviewedAt");
    Objects.requireNonNull(expiry, "expiry");
    if (expiry.isNegative() || expiry.isZero() || expiry.compareTo(
        Duration.ofDays(30)
    ) > 0) {
      throw new IllegalArgumentException("Rewrite proposal expiry is invalid");
    }
    RewriteCandidateReservation reservation = self.reservations
        .getRewriteCandidateReservation(proposal.proposalId());
    if (!proposal.input().equals(reservation.workerInput())) {
      throw new IllegalArgumentException(
          "Rewrite proposal does not match its reservation"
      );
    }
    Instant expiresAt = proposal.createdAt().plus(expiry);
    if (!reviewedAt.isBefore(expiresAt)) {
      return RewriteProposalReviewServiceUnavailable.unavailable(proposal, RewriteReviewState.EXPIRED, List.of(), expiresAt);
    }

    StyleAnalysisJob analysis = self.analyses.getStyleAnalysis(
        reservation.eligibility().analysisId()
    );
    StyleProfileVersionView profile = self.profiles.getStyleProfileVersion(
        reservation.eligibility().profileId(),
        reservation.eligibility().profileVersionId()
    );
    StoredRevision stored = self.revisions.getRevision(
        reservation.novelId(), reservation.eligibility().revisionId()
    );
    List<String> stale = self.staleReasons(reservation, stored, profile);
    if (!stale.isEmpty()) {
      return RewriteProposalReviewServiceUnavailable.unavailable(proposal, RewriteReviewState.STALE, stale, expiresAt);
    }

    return RewriteCurrentReview.evaluate(self, proposal, corpora, reviewedAt, expiresAt, reservation, analysis, profile, stored);
  }
}

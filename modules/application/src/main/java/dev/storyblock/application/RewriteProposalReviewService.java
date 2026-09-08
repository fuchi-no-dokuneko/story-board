package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RewriteProposalReviewService {
  public static final Duration DEFAULT_EXPIRY = Duration.ofDays(7);

  final RevisionStore revisions;
  final StyleAnalysisStore analyses;
  final StyleProfileStore profiles;
  final RewriteReservationStore reservations;
  final RewriteRiskEvaluator risks;
  final StyleFeatureAnalyzer style;

  public RewriteProposalReviewService(
      RevisionStore revisions,
      StyleAnalysisStore analyses,
      StyleProfileStore profiles,
      RewriteReservationStore reservations
  ) {
    this.revisions = Objects.requireNonNull(revisions, "revisions");
    this.analyses = Objects.requireNonNull(analyses, "analyses");
    this.profiles = Objects.requireNonNull(profiles, "profiles");
    this.reservations = Objects.requireNonNull(reservations, "reservations");
    this.risks = new RewriteRiskEvaluator();
    this.style = new StyleFeatureAnalyzer();
  }

  public RewriteProposalReview review(
      RewriteTextProposal proposal,
      List<RewriteReferenceCorpus> corpora,
      Instant reviewedAt,
      Duration expiry
  ) {
    return RewriteProposalReviewServiceReviewAction.review(this, proposal, corpora, reviewedAt, expiry);
  }

  List<String> staleReasons(
      RewriteCandidateReservation reservation,
      StoredRevision stored,
      StyleProfileVersionView profile
  ) {
    return RewriteProposalReviewServiceStaleReasonsAction.staleReasons(this, reservation, stored, profile);
  }

  StyleScores scores(
      StyleProfileVersionView profile,
      StyleAnalysisJob analysis,
      RewriteCandidateReservation reservation,
      RewriteTextProposal proposal
  ) {
    return RewriteProposalReviewServiceScoresAction.scores(this, profile, analysis, reservation, proposal);
  }

  record CandidateEdit(
      EditOperation operation,
      Ids.RevisionId candidateRevisionId
  ) {
  }

  record StyleScores(
      Map<String, Object> before,
      Map<String, Object> after
  ) {
  }
}

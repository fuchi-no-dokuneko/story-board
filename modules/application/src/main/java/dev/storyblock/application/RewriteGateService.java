package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCandidateReservationSaveResult;
import dev.storyblock.rewrite.policy.RewriteEligibilityPolicy;
import dev.storyblock.rewrite.policy.RewriteReservationStore;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.style.StyleAnalysisStore;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleProfileStore;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class RewriteGateService {
    final StyleAnalysisStore analyses;
    final StyleProfileStore profiles;
    final RewriteReservationStore reservations;
    final RewriteEligibilityPolicy eligibilityPolicy;
    final RevisionStore revisions;

    public RewriteGateService(
            StyleAnalysisStore analyses,
            StyleProfileStore profiles,
            RewriteReservationStore reservations,
            RevisionStore revisions
    ) {
        this(analyses, profiles, reservations, revisions, new RewriteEligibilityPolicy());
    }

    RewriteGateService(
            StyleAnalysisStore analyses,
            StyleProfileStore profiles,
            RewriteReservationStore reservations,
            RevisionStore revisions,
            RewriteEligibilityPolicy eligibilityPolicy
    ) {
        this.analyses = Objects.requireNonNull(analyses, "analyses");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.revisions = Objects.requireNonNull(revisions, "revisions");
        this.eligibilityPolicy = Objects.requireNonNull(
                eligibilityPolicy, "eligibilityPolicy"
        );
    }

    public RewriteCandidateReservationSaveResult reserve(
            Ids.StyleAnalysisId analysisId,
            String expectedAnalysisStatusHash,
            List<String> findingIds,
            Duration cooldown,
            String idempotencyKey,
            AuditContext auditContext
    ) {
        return RewriteGateServiceReserveAction.reserve(this, analysisId, expectedAnalysisStatusHash, findingIds, cooldown, idempotencyKey, auditContext);
    }

    public RewriteCandidateReservation get(Ids.ProposalId proposalId) {
        return reservations.getRewriteCandidateReservation(proposalId);
    }

    List<StyleAnalysisWindowFinding> selectFindings(
            Ids.StyleAnalysisId analysisId,
            List<String> findingIds
    ) {
        return RewriteGateServiceSelectFindingsAction.selectFindings(this, analysisId, findingIds);
    }

}

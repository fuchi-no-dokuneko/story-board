package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.rewrite.policy.ReserveRewriteCandidateCommand;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCandidateReservationSaveResult;
import dev.storyblock.rewrite.policy.RewriteEligibility;
import dev.storyblock.rewrite.policy.RewriteEligibilityException;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

final class RewriteGateServiceReserveAction {
    static RewriteCandidateReservationSaveResult reserve(RewriteGateService self, Ids.StyleAnalysisId analysisId, String expectedAnalysisStatusHash, List<String> findingIds, Duration cooldown, String idempotencyKey, AuditContext auditContext)  {
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(auditContext, "auditContext");
        RewriteGateServiceValidateCooldown.validateCooldown(cooldown);
        StyleAnalysisJob analysis = self.analyses.getStyleAnalysis(analysisId);
        if (!analysis.statusHash().equals(expectedAnalysisStatusHash)) {
            throw new RewriteEligibilityException(
                    "Style analysis changed before rewrite reservation"
            );
        }
        StyleProfileVersionView profile = self.profiles.getStyleProfileVersion(
                analysis.snapshot().profileVersion().profileId(),
                analysis.snapshot().profileVersion().versionId()
        );
        RewriteEligibility eligibility = self.eligibilityPolicy.evaluate(
                analysis,
                profile,
                self.selectFindings(analysisId, findingIds),
                auditContext.occurredAt()
        );
        RewriteWorkerInput input = RewriteGateServiceWorkerInput.workerInput(analysis, eligibility);
        RewriteCandidateReservation reservation = new RewriteCandidateReservation(
                eligibility,
                input,
                auditContext,
                auditContext.occurredAt().plus(cooldown)
        );
        return self.reservations.reserveRewriteCandidate(new ReserveRewriteCandidateCommand(
                reservation,
                idempotencyKey,
                ReserveRewriteCandidateCommand.hash(eligibility, cooldown)
        ));
    }
}

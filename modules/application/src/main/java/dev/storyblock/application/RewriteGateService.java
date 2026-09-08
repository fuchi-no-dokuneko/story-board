package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.rewrite.policy.ReserveRewriteCandidateCommand;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCandidateReservationSaveResult;
import dev.storyblock.rewrite.policy.RewriteEligibility;
import dev.storyblock.rewrite.policy.RewriteEligibilityException;
import dev.storyblock.rewrite.policy.RewriteEligibilityPolicy;
import dev.storyblock.rewrite.policy.RewriteReservationStore;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisStore;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleAnalysisWindowSlice;
import dev.storyblock.style.StyleProfileStore;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RewriteGateService {
    private final StyleAnalysisStore analyses;
    private final StyleProfileStore profiles;
    private final RewriteReservationStore reservations;
    private final RewriteEligibilityPolicy eligibilityPolicy;

    public RewriteGateService(
            StyleAnalysisStore analyses,
            StyleProfileStore profiles,
            RewriteReservationStore reservations
    ) {
        this(analyses, profiles, reservations, new RewriteEligibilityPolicy());
    }

    RewriteGateService(
            StyleAnalysisStore analyses,
            StyleProfileStore profiles,
            RewriteReservationStore reservations,
            RewriteEligibilityPolicy eligibilityPolicy
    ) {
        this.analyses = Objects.requireNonNull(analyses, "analyses");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.reservations = Objects.requireNonNull(reservations, "reservations");
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
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(auditContext, "auditContext");
        RewriteGateServiceValidateCooldown.validateCooldown(cooldown);
        StyleAnalysisJob analysis = analyses.getStyleAnalysis(analysisId);
        if (!analysis.statusHash().equals(expectedAnalysisStatusHash)) {
            throw new RewriteEligibilityException(
                    "Style analysis changed before rewrite reservation"
            );
        }
        StyleProfileVersionView profile = profiles.getStyleProfileVersion(
                analysis.snapshot().profileVersion().profileId(),
                analysis.snapshot().profileVersion().versionId()
        );
        RewriteEligibility eligibility = eligibilityPolicy.evaluate(
                analysis,
                profile,
                selectFindings(analysisId, findingIds),
                auditContext.occurredAt()
        );
        RewriteWorkerInput input = RewriteGateServiceWorkerInput.workerInput(analysis, eligibility);
        RewriteCandidateReservation reservation = new RewriteCandidateReservation(
                eligibility,
                input,
                auditContext,
                auditContext.occurredAt().plus(cooldown)
        );
        return reservations.reserveRewriteCandidate(new ReserveRewriteCandidateCommand(
                reservation,
                idempotencyKey,
                ReserveRewriteCandidateCommand.hash(eligibility, cooldown)
        ));
    }

    public RewriteCandidateReservation get(Ids.ProposalId proposalId) {
        return reservations.getRewriteCandidateReservation(proposalId);
    }

    private List<StyleAnalysisWindowFinding> selectFindings(
            Ids.StyleAnalysisId analysisId,
            List<String> findingIds
    ) {
        findingIds = List.copyOf(findingIds);
        if (findingIds.isEmpty() || findingIds.size() > RewriteModule.MAX_FINDINGS
                || new HashSet<>(findingIds).size() != findingIds.size()) {
            throw new RewriteEligibilityException(
                    "Rewrite finding selection must be nonempty and unique"
            );
        }
        Set<String> requested = Set.copyOf(findingIds);
        List<StyleAnalysisWindowFinding> selected = new ArrayList<>();
        int after = -1;
        while (true) {
            StyleAnalysisWindowSlice page = analyses.listStyleAnalysisWindows(
                    analysisId, after, 200
            );
            page.items().stream()
                    .filter(value -> requested.contains(value.windowId()))
                    .forEach(selected::add);
            if (page.nextOrdinal() == null) {
                break;
            }
            after = page.nextOrdinal();
        }
        if (selected.size() != requested.size()) {
            throw new RewriteEligibilityException(
                    "One or more selected rewrite findings do not exist"
            );
        }
        return List.copyOf(selected);
    }

}

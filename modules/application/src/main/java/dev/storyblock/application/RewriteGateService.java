package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
import dev.storyblock.rewrite.RewriteConstraints;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.rewrite.RewriteSourceBlock;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.rewrite.policy.ReserveRewriteCandidateCommand;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCandidateReservationSaveResult;
import dev.storyblock.rewrite.policy.RewriteEligibility;
import dev.storyblock.rewrite.policy.RewriteEligibilityException;
import dev.storyblock.rewrite.policy.RewriteEligibilityPolicy;
import dev.storyblock.rewrite.policy.RewritePolicyModule;
import dev.storyblock.rewrite.policy.RewriteReservationStore;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleAnalysisBlock;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisStore;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleAnalysisWindowSlice;
import dev.storyblock.style.StyleFeatureChannel;
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
        validateCooldown(cooldown);
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
        RewriteWorkerInput input = workerInput(analysis, eligibility);
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

    private static RewriteWorkerInput workerInput(
            StyleAnalysisJob analysis,
            RewriteEligibility eligibility
    ) {
        List<StyleAnalysisBlock> snapshot = analysis.snapshot().blocks();
        Ids.BlockId firstId = eligibility.affectedBlockIds().getFirst();
        Ids.BlockId lastId = eligibility.affectedBlockIds().getLast();
        int first = indexOf(snapshot, firstId);
        int last = indexOf(snapshot, lastId);
        int from = Math.max(0, first - RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE);
        int to = Math.min(
                snapshot.size(), last + RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE + 1
        );
        List<RewriteSourceBlock> blocks = new ArrayList<>();
        for (int index = from; index < to; index++) {
            var block = snapshot.get(index).block();
            blocks.add(RewriteSourceBlock.create(
                    block.id(), block.versionId(), block.text(),
                    index >= first && index <= last
            ));
        }
        List<String> directives = eligibility.decisions().stream()
                .flatMap(decision -> decision.independentQ99Channels().stream())
                .distinct()
                .sorted(java.util.Comparator.comparing(Enum::ordinal))
                .map(RewriteGateService::directive)
                .toList();
        int editableCount = eligibility.affectedBlockIds().size();
        return new RewriteWorkerInput(
                Ids.ProposalId.create(),
                eligibility.analysisId(),
                eligibility.novelId(),
                eligibility.revisionId(),
                eligibility.revisionHash(),
                eligibility.profileVersionId(),
                eligibility.profileVersionHash(),
                eligibility.analyzerContractHash(),
                eligibility.windowConfigurationHash(),
                eligibility.findingIds(),
                blocks,
                new RewriteConstraints(
                        editableCount,
                        editableCount * UnicodeText.MAX_BLOCK_GRAPHEMES,
                        directives
                )
        );
    }

    private static String directive(StyleFeatureChannel channel) {
        return "Reduce " + channel.canonicalName()
                + " style deviation while preserving facts and metadata.";
    }

    private static int indexOf(
            List<StyleAnalysisBlock> blocks,
            Ids.BlockId blockId
    ) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).block().id().equals(blockId)) {
                return index;
            }
        }
        throw new RewriteEligibilityException(
                "Rewrite affected block is outside the analysis snapshot"
        );
    }

    private static void validateCooldown(Duration cooldown) {
        Objects.requireNonNull(cooldown, "cooldown");
        if (cooldown.compareTo(RewritePolicyModule.MIN_COOLDOWN) < 0
                || cooldown.compareTo(RewritePolicyModule.MAX_COOLDOWN) > 0) {
            throw new IllegalArgumentException("Rewrite cooldown duration is invalid");
        }
    }
}

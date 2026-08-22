package dev.storyblock.application;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteReferenceCorpus;
import dev.storyblock.rewrite.policy.RewriteReservationStore;
import dev.storyblock.rewrite.policy.RewriteRiskAssessment;
import dev.storyblock.rewrite.policy.RewriteRiskEvaluator;
import dev.storyblock.rewrite.policy.RewriteRiskState;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisStore;
import dev.storyblock.style.StyleDistanceReport;
import dev.storyblock.style.StyleFeatureAnalyzer;
import dev.storyblock.style.StyleFeatureSet;
import dev.storyblock.style.StyleProfileStore;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RewriteProposalReviewService {
    public static final Duration DEFAULT_EXPIRY = Duration.ofDays(7);

    private final RevisionStore revisions;
    private final StyleAnalysisStore analyses;
    private final StyleProfileStore profiles;
    private final RewriteReservationStore reservations;
    private final RewriteRiskEvaluator risks;
    private final StyleFeatureAnalyzer style;

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
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(reviewedAt, "reviewedAt");
        Objects.requireNonNull(expiry, "expiry");
        if (expiry.isNegative() || expiry.isZero() || expiry.compareTo(
                Duration.ofDays(30)
        ) > 0) {
            throw new IllegalArgumentException("Rewrite proposal expiry is invalid");
        }
        RewriteCandidateReservation reservation = reservations
                .getRewriteCandidateReservation(proposal.proposalId());
        if (!proposal.input().equals(reservation.workerInput())) {
            throw new IllegalArgumentException(
                    "Rewrite proposal does not match its reservation"
            );
        }
        Instant expiresAt = proposal.createdAt().plus(expiry);
        if (!reviewedAt.isBefore(expiresAt)) {
            return unavailable(proposal, RewriteReviewState.EXPIRED, List.of(), expiresAt);
        }

        StyleAnalysisJob analysis = analyses.getStyleAnalysis(
                reservation.eligibility().analysisId()
        );
        StyleProfileVersionView profile = profiles.getStyleProfileVersion(
                reservation.eligibility().profileId(),
                reservation.eligibility().profileVersionId()
        );
        StoredRevision stored = revisions.getRevision(
                reservation.novelId(), reservation.eligibility().revisionId()
        );
        List<String> stale = staleReasons(reservation, stored, profile);
        if (!stale.isEmpty()) {
            return unavailable(proposal, RewriteReviewState.STALE, stale, expiresAt);
        }

        List<NarrativeBlock> sourceInput = exactInputBlocks(
                proposal, analysis.snapshot().blocks().stream()
                        .map(value -> value.block()).toList()
        );
        RewriteRiskAssessment risk = risks.evaluate(
                proposal,
                sourceInput,
                analysis.snapshot().maskingLexicon(),
                profile,
                corpora
        );
        CandidateEdit edit = candidateEdit(stored, reservation, proposal, reviewedAt);
        PreviewResponse preview = new PreviewService(revisionId -> revisions
                .getRevision(reservation.novelId(), revisionId).manifest())
                .preview(
                        stored.manifest(),
                        edit.operation(),
                        edit.candidateRevisionId(),
                        reviewedAt
                );
        StyleScores scores = scores(
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
        );
    }

    private static RewriteProposalReview unavailable(
            RewriteTextProposal proposal,
            RewriteReviewState state,
            List<String> staleReasons,
            Instant expiresAt
    ) {
        return new RewriteProposalReview(
                proposal.proposalId(), proposal.proposalHash(), state,
                staleReasons, null, null, Map.of(), Map.of(), null, null, expiresAt
        );
    }

    private List<String> staleReasons(
            RewriteCandidateReservation reservation,
            StoredRevision stored,
            StyleProfileVersionView profile
    ) {
        List<String> reasons = new ArrayList<>();
        RevisionRef head = revisions.getHead(reservation.novelId());
        if (!head.revisionId().equals(reservation.eligibility().revisionId())
                || !head.contentHash().equals(reservation.eligibility().revisionHash())) {
            reasons.add("head_changed");
        }
        if (!stored.contentHash().equals(reservation.eligibility().revisionHash())) {
            reasons.add("revision_changed");
        }
        if (!profile.canGateRewrites()
                || !profile.profileVersion().versionHash().equals(
                        reservation.eligibility().profileVersionHash()
                )) {
            reasons.add("profile_changed");
        }
        Map<Ids.BlockId, NarrativeBlock> current = blocks(stored);
        reservation.workerInput().blocks().forEach(binding -> {
            NarrativeBlock block = current.get(binding.blockId());
            if (block == null) {
                reasons.add("affected_block_missing");
            } else if (!block.versionId().equals(binding.blockVersionId())
                    || !block.text().equals(binding.text())) {
                reasons.add("affected_block_changed");
            }
        });
        return reasons.stream().distinct().sorted().toList();
    }

    private static CandidateEdit candidateEdit(
            StoredRevision stored,
            RewriteCandidateReservation reservation,
            RewriteTextProposal proposal,
            Instant reviewedAt
    ) {
        List<Ids.BlockId> affected = reservation.eligibility().affectedBlockIds();
        NarrativeScene scene = null;
        for (NarrativeChapter chapter : stored.manifest().novel().chapters()) {
            for (NarrativeScene candidate : chapter.scenes()) {
                if (candidate.blocks().stream().anyMatch(block ->
                        block.id().equals(affected.getFirst()))) {
                    scene = candidate;
                }
            }
        }
        if (scene == null || !scene.blocks().stream().map(NarrativeBlock::id)
                .toList().containsAll(affected)) {
            throw new IllegalArgumentException(
                    "Rewrite affected range must remain inside one scene"
            );
        }
        NarrativeScene selectedScene = scene;
        Map<Ids.BlockId, RewriteCandidateBlock> replacements = new HashMap<>();
        proposal.candidates().forEach(value -> replacements.put(value.blockId(), value));
        List<BlockDraft> drafts = affected.stream().map(blockId -> {
            NarrativeBlock source = selectedScene.blocks().stream()
                    .filter(block -> block.id().equals(blockId)).findFirst().orElseThrow();
            RewriteCandidateBlock replacement = replacements.get(blockId);
            return new BlockDraft(
                    source.id(),
                    replacement == null ? source.text() : replacement.proposedText(),
                    source.metadata(),
                    source.extensions()
            );
        }).toList();
        EditContext context = new EditContext(
                Ids.OperationId.create(),
                "rewrite:" + proposal.proposalId().value(),
                reservation.novelId(),
                reservation.eligibility().revisionId(),
                reservation.eligibility().revisionHash()
        );
        Ids.RevisionId candidateRevisionId = Ids.RevisionId.create();
        return new CandidateEdit(
                new EditOperation.ReplaceBlockRange(
                        context,
                        BlockRangeGuard.capture(
                                selectedScene, affected.getFirst(), affected.getLast()
                        ),
                        drafts
                ),
                candidateRevisionId
        );
    }

    private StyleScores scores(
            StyleProfileVersionView profile,
            StyleAnalysisJob analysis,
            RewriteCandidateReservation reservation,
            RewriteTextProposal proposal
    ) {
        Map<Ids.BlockId, RewriteCandidateBlock> replacements = new HashMap<>();
        proposal.candidates().forEach(value -> replacements.put(value.blockId(), value));
        List<NarrativeBlock> beforeBlocks = analysis.snapshot().blocks().stream()
                .map(value -> value.block())
                .filter(block -> reservation.eligibility().affectedBlockIds()
                        .contains(block.id()))
                .toList();
        List<NarrativeBlock> afterBlocks = beforeBlocks.stream().map(block -> {
            RewriteCandidateBlock replacement = replacements.get(block.id());
            return replacement == null ? block : block.revise(
                    replacement.proposedText(),
                    block.metadata(),
                    block.extensions(),
                    block.versionId()
            );
        }).toList();
        StyleFeatureSet target = profile.profileVersion().content().featureSet();
        StyleFeatureSet before = style.extract(
                beforeBlocks,
                analysis.snapshot().maskingLexicon(),
                target.contract()
        );
        StyleFeatureSet after = style.extract(
                afterBlocks,
                analysis.snapshot().maskingLexicon(),
                target.contract()
        );
        StyleDistanceReport beforeScore = style.compare(target, before);
        StyleDistanceReport afterScore = style.compare(target, after);
        return new StyleScores(
                beforeScore.canonicalValue(), afterScore.canonicalValue()
        );
    }

    private static List<NarrativeBlock> exactInputBlocks(
            RewriteTextProposal proposal,
            List<NarrativeBlock> snapshot
    ) {
        Map<Ids.BlockId, NarrativeBlock> values = new HashMap<>();
        snapshot.forEach(block -> values.put(block.id(), block));
        return proposal.input().blocks().stream().map(binding -> {
            NarrativeBlock block = values.get(binding.blockId());
            if (block == null) {
                throw new IllegalArgumentException(
                        "Rewrite input block is outside its analysis snapshot"
                );
            }
            return block;
        }).toList();
    }

    private static Map<Ids.BlockId, NarrativeBlock> blocks(StoredRevision revision) {
        Map<Ids.BlockId, NarrativeBlock> result = new HashMap<>();
        revision.manifest().novel().chapters().forEach(chapter ->
                chapter.scenes().forEach(scene ->
                        scene.blocks().forEach(block -> result.put(block.id(), block))
                )
        );
        return result;
    }

    private record CandidateEdit(
            EditOperation operation,
            Ids.RevisionId candidateRevisionId
    ) {
    }

    private record StyleScores(
            Map<String, Object> before,
            Map<String, Object> after
    ) {
    }
}

package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.List;
import java.util.Set;

public final class RewriteRiskEvaluator {
    static final Set<ProtectedFactKind> SOURCE_MANUAL_RISK = Set.of(
            ProtectedFactKind.NUMBER,
            ProtectedFactKind.NEGATION,
            ProtectedFactKind.CAUSALITY,
            ProtectedFactKind.SPEAKER,
            ProtectedFactKind.PRESENCE,
            ProtectedFactKind.EVIDENCE,
            ProtectedFactKind.HIGH_RISK_METADATA
    );

    final RewriteProtectedFactExtractor facts;
    final RewriteNearCopyChecker nearCopy;

    public RewriteRiskEvaluator() {
        this(new RewriteProtectedFactExtractor(), new RewriteNearCopyChecker());
    }

    RewriteRiskEvaluator(
            RewriteProtectedFactExtractor facts,
            RewriteNearCopyChecker nearCopy
    ) {
        this.facts = java.util.Objects.requireNonNull(facts, "facts");
        this.nearCopy = java.util.Objects.requireNonNull(nearCopy, "nearCopy");
    }

    public RewriteRiskAssessment evaluate(
            RewriteTextProposal proposal,
            List<NarrativeBlock> sourceBlocks,
            StyleMaskingLexicon lexicon,
            StyleProfileVersionView currentProfile,
            List<RewriteReferenceCorpus> corpora
    ) {
        return RewriteRiskEvaluatorEvaluateAction.evaluate(this, proposal, sourceBlocks, lexicon, currentProfile, corpora);
    }

    record FactKey(ProtectedFactKind kind, String valueHash) {
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleDistanceReport;
import dev.storyblock.style.StyleFeatureSet;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.application.RewriteProposalReviewService.StyleScores;

final class RewriteProposalReviewServiceScoresAction {
    static StyleScores scores(RewriteProposalReviewService self, StyleProfileVersionView profile, StyleAnalysisJob analysis, RewriteCandidateReservation reservation, RewriteTextProposal proposal)  {
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
        StyleFeatureSet before = self.style.extract(
                beforeBlocks,
                analysis.snapshot().maskingLexicon(),
                target.contract()
        );
        StyleFeatureSet after = self.style.extract(
                afterBlocks,
                analysis.snapshot().maskingLexicon(),
                target.contract()
        );
        StyleDistanceReport beforeScore = self.style.compare(target, before);
        StyleDistanceReport afterScore = self.style.compare(target, after);
        return new StyleScores(
                beforeScore.canonicalValue(), afterScore.canonicalValue()
        );
    }
}

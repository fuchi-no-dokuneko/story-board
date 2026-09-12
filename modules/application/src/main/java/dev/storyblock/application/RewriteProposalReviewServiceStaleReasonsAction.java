package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RewriteProposalReviewServiceStaleReasonsAction {
    static List<String> staleReasons(RewriteProposalReviewService self, RewriteCandidateReservation reservation, StoredRevision stored, StyleProfileVersionView profile)  {
        List<String> reasons = new ArrayList<>();
        RevisionRef head = self.revisions.getHead(reservation.novelId());
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
        Map<Ids.BlockId, NarrativeBlock> current = RewriteProposalReviewServiceBlocks.blocks(stored);
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
}

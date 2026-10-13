package dev.storyblock.application;

import dev.storyblock.domain.*;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.storage.StoredRevision;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.application.RewriteProposalReviewService.CandidateEdit;

final class RewriteProposalReviewServiceCandidateEdit {
  static CandidateEdit candidateEdit(StoredRevision stored, RewriteCandidateReservation reservation, RewriteTextProposal proposal, Instant reviewedAt)  {
    List<Ids.BlockId> affected = reservation.eligibility().affectedBlockIds();
    BlockRangeGuard guard = RewriteVisibleRange.require(stored.manifest(), affected);
    NarrativeScene selectedScene = stored.manifest().requireScene(guard.sceneId());
    Map<Ids.BlockId, RewriteCandidateBlock> replacements = new HashMap<>();
    proposal.candidates().forEach(value -> replacements.put(value.blockId(), value));
    List<BlockDraft> drafts = affected.stream().map(blockId -> {
      NarrativeBlock source = NarrativeView.visibleBlocks(selectedScene).stream()
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
            guard,
            drafts
        ),
        candidateRevisionId
    );
  }
}

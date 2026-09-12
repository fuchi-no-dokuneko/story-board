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
}

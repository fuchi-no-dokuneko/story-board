package dev.storyblock.rewrite;

import dev.storyblock.domain.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static dev.storyblock.rewrite.RewriteTextProposal.*;

final class RewriteTextProposalValidation {
  static void validate(RewriteWorkerInput input, List<RewriteCandidateBlock> candidates, Instant createdAt) {
    if (candidates.isEmpty()
            || candidates.size() > input.constraints().maxChangedBlocks()
            || new HashSet<>(candidates.stream().map(
                RewriteCandidateBlock::blockId
            ).toList()).size() != candidates.size()) {
          throw new IllegalArgumentException("Rewrite proposal candidates are invalid");
        }
        Map<Ids.BlockId, RewriteSourceBlock> sources = new HashMap<>();
        input.blocks().forEach(block -> sources.put(block.blockId(), block));
        Map<Ids.BlockId, Integer> sourceOrdinals = new HashMap<>();
        for (int index = 0; index < input.blocks().size(); index++) {
          sourceOrdinals.put(input.blocks().get(index).blockId(), index);
        }
        int proposedGraphemes = 0;
        int priorOrdinal = -1;
        for (RewriteCandidateBlock candidate : candidates) {
          RewriteSourceBlock source = sources.get(candidate.blockId());
          Integer sourceOrdinal = sourceOrdinals.get(candidate.blockId());
          if (source == null || !source.editable()
              || !candidate.sourceBlockVersionId().equals(source.blockVersionId())
              || !candidate.sourceTextHash().equals(source.textHash())
              || sourceOrdinal == null || sourceOrdinal <= priorOrdinal) {
            throw new IllegalArgumentException(
                "Rewrite candidate is not canonically bound to its source block"
            );
          }
          priorOrdinal = sourceOrdinal;
          proposedGraphemes += UnicodeText.graphemeCount(candidate.proposedText());
        }
        if (proposedGraphemes > input.constraints().maxOutputGraphemes()) {
          throw new IllegalArgumentException("Rewrite proposal exceeds its output limit");
        }
        Objects.requireNonNull(createdAt, "createdAt");
  }
}

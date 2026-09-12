package dev.storyblock.monitor;
import dev.storyblock.domain.Ids;
import java.util.*;
final class MonitorOutputBindingValidation {
  static void validate(List<MonitorBlockFingerprint> affectedBlocks, MonitorOutput output, Ids.NovelId novelId, Ids.RevisionId revisionId, String revisionHash) {
    Set<Ids.BlockId> affectedIds = affectedBlocks.stream()
        .map(MonitorBlockFingerprint::blockId)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    Set<Ids.BlockId> evidenceIds = output.evidence().stream()
        .map(MonitorEvidence::blockId)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    if (!affectedIds.equals(evidenceIds)) {
      throw new IllegalArgumentException(
          "Every stored affected monitor block must have evidence"
      );
    }
    if (output instanceof MonitorProposedOperation proposal) {
      if (!proposal.operation().context().novelId().equals(novelId)
          || !proposal.operation().context().baseRevisionId().equals(revisionId)
          || !proposal.operation().context().expectedHeadHash().equals(revisionHash)) {
        throw new IllegalArgumentException(
            "Stored monitor proposal context must match its source revision"
        );
      }
      if (proposal.operation()
          instanceof dev.storyblock.domain.EditOperation.RestoreRevisionContent) {
        throw new IllegalArgumentException(
            "Stored monitor proposals cannot restore global revision content"
        );
      }
    }
  }
}

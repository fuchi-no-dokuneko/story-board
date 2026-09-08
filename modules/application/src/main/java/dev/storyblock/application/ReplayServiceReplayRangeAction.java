package dev.storyblock.application;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.*;
import dev.storyblock.storage.StoredOperation;
import dev.storyblock.storage.StoredRevision;
import java.util.List;
import java.util.Map;

final class ReplayServiceReplayRangeAction {
  static ReplayResult replayRange(ReplayService self, Ids.NovelId novelId, RevisionManifest start, long startingSequence, StoredRevision target, RevisionLookup revisionLookup, Map<Ids.RevisionId, RevisionManifest> replayed)  {
    if (startingSequence > target.sequence()) {
      throw ReplayServiceFailure.failure(novelId, target.sequence(), "Replay starts after target revision");
    }
    List<StoredOperation> operations = self.store.listOperations(
        novelId, startingSequence, target.sequence()
    );
    long requiredCount = target.sequence() - startingSequence;
    if (operations.size() != requiredCount) {
      throw ReplayServiceFailure.failure(
          novelId,
          target.sequence(),
          "Operation log has a gap: expected " + requiredCount
              + " entries but found " + operations.size()
      );
    }

    RevisionManifest current = start;
    String currentHash = NarrativeCanonicalMapper.toCanonical(current).contentHash();
    NarrativeEditor editor = new NarrativeEditor(revisionLookup);
    long expectedSequence = startingSequence + 1;
    for (StoredOperation stored : operations) {
      current = ReplayStep.apply(novelId, expectedSequence, stored, current, currentHash, editor);
      currentHash = NarrativeCanonicalMapper.toCanonical(current).contentHash();
      if (!currentHash.equals(stored.resultHash())) {
        throw ReplayServiceFailure.failure(
            novelId,
            expectedSequence,
            "Operation result hash does not match replayed content"
        );
      }
      StoredRevision relational = self.store.getRevisionAtSequence(novelId, expectedSequence);
      if (!relational.manifest().equals(current)
          || !relational.contentHash().equals(currentHash)) {
        throw ReplayServiceFailure.failure(
            novelId,
            expectedSequence,
            "Stored revision does not match operation replay"
        );
      }
      replayed.put(current.id(), current);
      expectedSequence++;
    }

    if (!current.id().equals(target.manifest().id())
        || !currentHash.equals(target.contentHash())) {
      throw ReplayServiceFailure.failure(
          novelId, target.sequence(), "Replay did not reproduce the target revision"
      );
    }
    return new ReplayResult(
        current,
        currentHash,
        target.sequence(),
        startingSequence,
        requiredCount
    );
  }
}

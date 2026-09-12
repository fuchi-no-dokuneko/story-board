package dev.storyblock.application;
import dev.storyblock.domain.*;
import dev.storyblock.storage.StoredOperation;

final class ReplayStep {
  static RevisionManifest apply(Ids.NovelId novelId, long expectedSequence, StoredOperation stored, RevisionManifest current, String currentHash, NarrativeEditor editor) {
      if (stored.sequence() != expectedSequence) {
        throw ReplayServiceFailure.failure(novelId, expectedSequence, "Operation sequence is not contiguous");
      }
      EditOperation operation = stored.operation();
      if (!operation.context().novelId().equals(novelId)
          || !operation.context().baseRevisionId().equals(current.id())
          || !operation.context().expectedHeadHash().equals(currentHash)) {
        throw ReplayServiceFailure.failure(
            novelId,
            expectedSequence,
            "Operation base identity or hash does not match replay state"
        );
      }
      try {
        current = editor.apply(
            current,
            operation,
            stored.resultRevisionId(),
            stored.committedAt()
        );
      } catch (RuntimeException exception) {
        throw new ReplayException(
            novelId,
            expectedSequence,
            "Operation could not be replayed",
            exception
        );
      }
      return current;
  }
}

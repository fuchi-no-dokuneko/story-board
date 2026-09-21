package dev.storyblock.application;
import dev.storyblock.domain.*;
import dev.storyblock.storage.StoredOperation;

final class ReplayStep {
  static RevisionManifest apply(Ids.NovelId novelId, long expectedSequence, StoredOperation stored, RevisionManifest current, String currentHash, NarrativeEditor editor, RevisionManifest expected) {
      if (stored.sequence() != expectedSequence) {
        throw ReplayServiceFailure.failure(novelId, expectedSequence, "Operation sequence is not contiguous");
      }
      EditOperation operation = stored.operation();
      if (!stored.resultRevisionId().equals(expected.id())
          || !stored.committedAt().equals(expected.createdAt()))
          throw ReplayServiceFailure.failure(novelId, expectedSequence, "Replay result identity differs");
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
        current = ReplayApplication.apply(editor,
            current,
            operation,
            expected
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

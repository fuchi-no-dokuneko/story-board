package dev.storyblock.storage;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.util.List;
import java.util.Optional;

public interface RevisionStore {
    void createNovel(RevisionManifest initialRevision, String contentHash);

    List<Ids.NovelId> listNovels();

    RevisionRef getHead(Ids.NovelId novelId);

    StoredRevision getRevision(Ids.NovelId novelId, Ids.RevisionId revisionId);

    StoredRevision getRevisionAtSequence(Ids.NovelId novelId, long sequence);

    Optional<StoredOperation> findByIdempotencyKey(Ids.NovelId novelId, String key);

    Optional<StoredCheckpoint> loadCheckpoint(
            Ids.NovelId novelId,
            long atOrBeforeSequence
    );

    byte[] decompressCheckpoint(StoredCheckpoint checkpoint);

    List<StoredOperation> listOperations(
            Ids.NovelId novelId,
            long afterSequence,
            long throughSequence
    );

    List<BlockTombstone> listTombstones(Ids.NovelId novelId);

    CommitResult commitCas(CommitRequest request);

    long revisionCount(Ids.NovelId novelId);

    long operationCount(Ids.NovelId novelId);
}

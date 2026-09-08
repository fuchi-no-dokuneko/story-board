package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.util.List;
import java.util.Optional;

interface SqliteRevisionReadPort extends RevisionStore, SqliteStoreContext {
  @Override
  default List<Ids.NovelId> listNovels() {
    return SqliteRevisionStoreListNovelsAction.listNovels(context());
  }

  @Override
  default RevisionRef getHead(Ids.NovelId novelId) {
    return context().read(connection -> SqliteRevisionStoreRequireHead.requireHead(connection, novelId));
  }

  @Override
  default StoredRevision getRevision(Ids.NovelId novelId, Ids.RevisionId revisionId) {
    return SqliteRevisionStoreGetRevisionAction.getRevision(context(), novelId, revisionId);
  }

  @Override
  default StoredRevision getRevisionAtSequence(Ids.NovelId novelId, long sequence) {
    return SqliteRevisionStoreGetRevisionAtSequenceAction.getRevisionAtSequence(context(), novelId, sequence);
  }

  @Override
  default Optional<StoredOperation> findByIdempotencyKey(Ids.NovelId novelId, String key) {
    return context().read(connection -> SqliteRevisionStoreFindByIdempotencyKeyFactory.findByIdempotencyKey(connection, novelId, key));
  }

  @Override
  default Optional<StoredCheckpoint> loadCheckpoint(
      Ids.NovelId novelId,
      long atOrBeforeSequence
  ) {
    return SqliteRevisionStoreLoadCheckpointAction.loadCheckpoint(context(), novelId, atOrBeforeSequence);
  }

  @Override
  default byte[] decompressCheckpoint(StoredCheckpoint checkpoint) {
    if (!GzipCheckpointCodec.NAME.equals(checkpoint.codec())) {
      throw new StorageException("Unsupported checkpoint codec " + checkpoint.codec());
    }
    return GzipCheckpointCodec.decompress(
        checkpoint.compressedCanonicalJson(), checkpoint.uncompressedBytes()
    );
  }

  @Override
  default List<StoredOperation> listOperations(
      Ids.NovelId novelId,
      long afterSequence,
      long throughSequence
  ) {
    return SqliteRevisionStoreListOperationsAction.listOperations(context(), novelId, afterSequence, throughSequence);
  }

  @Override
  default List<BlockTombstone> listTombstones(Ids.NovelId novelId) {
    return SqliteRevisionStoreListTombstonesAction.listTombstones(context(), novelId);
  }

  @Override
  default long revisionCount(Ids.NovelId novelId) {
    return SqliteRevisionStoreCountAction.count(context(), novelId, "revisions");
  }

  @Override
  default long operationCount(Ids.NovelId novelId) {
    return SqliteRevisionStoreCountAction.count(context(), novelId, "operations");
  }
}

package dev.storyblock.application;

import dev.storyblock.cache.MemoCache;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.*;

/** Every access checks the persisted head; commits/imports/deletions stay visible. */
public final class NovelReadCache {
    private final RevisionStore store;
    private final MemoCache<Key, StoredRevision> revisions = new MemoCache<>(128);
    public NovelReadCache(RevisionStore store) { this.store = store; }
    public Read read(Ids.NovelId novel, Ids.RevisionId requested) {
        var head = store.getHead(novel);
        var revision = requested == null ? head.revisionId() : requested;
        var key = new Key(novel, head, revision);
        return new Read(key, revisions.get(key, () -> store.getRevision(novel, revision)));
    }
    public MemoCache.Snapshot snapshot() { return revisions.snapshot(); }
    public record Key(Ids.NovelId novel, RevisionRef head, Ids.RevisionId revision) { }
    public record Read(Key key, StoredRevision revision) { }
}

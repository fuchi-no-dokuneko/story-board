package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionStore;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class NovelCatalogService {
    private final RevisionStore store;
    private final NovelReadCache reads;
    private final dev.storyblock.cache.MemoCache<NovelReadCache.Key, NovelCatalogSnapshot> entries = new dev.storyblock.cache.MemoCache<>(128);

    public NovelCatalogService(RevisionStore store) {
        this(store, new NovelReadCache(store));
    }

    public NovelCatalogService(RevisionStore store, NovelReadCache reads) {
        this.store = Objects.requireNonNull(store, "store"); this.reads = reads;
    }

    public List<NovelCatalogEntry> list(String query) {
        String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return store.listNovels().stream()
                .map(this::get)
                .filter(entry -> normalizedQuery.isEmpty()
                        || entry.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || entry.novelId().value().contains(normalizedQuery)
                        || entry.language().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || entry.mainCharacters().stream().anyMatch(character ->
                                character.toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .sorted(java.util.Comparator
                        .comparing(NovelCatalogEntry::updatedAt).reversed()
                        .thenComparing(entry -> entry.novelId().value()))
                .toList();
    }

    public NovelCatalogEntry get(Ids.NovelId novelId) {
        return read(novelId).novel();
    }

    public CanonicalRevision revision(Ids.NovelId novelId) { return read(novelId).revision(); }

    public NovelCatalogSnapshot read(Ids.NovelId novelId) {
        var value = reads.read(novelId, null);
        return entries.get(value.key(), () -> new NovelCatalogSnapshot(
                NovelCatalogServiceDescribe.describe(value.revision().manifest(), value.key().head()),
                NarrativeCanonicalMapper.toCanonical(value.revision().manifest())));
    }
}

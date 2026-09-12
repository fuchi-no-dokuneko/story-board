package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredRevision;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class NovelCatalogService {
    private final RevisionStore store;

    public NovelCatalogService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
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
        RevisionRef head = store.getHead(novelId);
        StoredRevision stored = store.getRevision(novelId, head.revisionId());
        return NovelCatalogServiceDescribe.describe(stored.manifest(), head);
    }

    public CanonicalRevision revision(Ids.NovelId novelId) {
        RevisionRef head = store.getHead(novelId);
        return NarrativeCanonicalMapper.toCanonical(
                store.getRevision(novelId, head.revisionId()).manifest()
        );
    }

}

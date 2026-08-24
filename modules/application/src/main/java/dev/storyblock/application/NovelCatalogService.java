package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredRevision;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        return describe(stored.manifest(), head);
    }

    public CanonicalRevision revision(Ids.NovelId novelId) {
        RevisionRef head = store.getHead(novelId);
        return NarrativeCanonicalMapper.toCanonical(
                store.getRevision(novelId, head.revisionId()).manifest()
        );
    }

    private static NovelCatalogEntry describe(
            RevisionManifest manifest,
            RevisionRef head
    ) {
        NarrativeNovel novel = manifest.novel();
        int sceneCount = novel.chapters().stream()
                .mapToInt(chapter -> chapter.scenes().size())
                .sum();
        String text = novel.chapters().stream()
                .flatMap(chapter -> chapter.scenes().stream())
                .flatMap(scene -> scene.blocks().stream())
                .map(block -> block.text())
                .collect(java.util.stream.Collectors.joining());
        Map<String, Object> extensions = novel.extensions();
        return new NovelCatalogEntry(
                novel.id(),
                title(novel),
                stringExtension(extensions, "language", "und"),
                head.revisionId(),
                head.sequence(),
                head.contentHash(),
                manifest.createdAt(),
                novel.chapters().size(),
                sceneCount,
                manifest.liveBlocks().size(),
                HanText.count(text),
                HanText.sha256(text),
                stringListExtension(extensions, "main-characters"),
                intExtension(extensions, "zombie-count"),
                intExtension(extensions, "tnt-cannon-count"),
                Boolean.TRUE.equals(extensions.get("agent-write-registered"))
        );
    }

    private static String title(NarrativeNovel novel) {
        Object configured = novel.extensions().get("title");
        if (configured instanceof String title && !title.isBlank()) {
            return title;
        }
        return novel.chapters().stream()
                .map(NarrativeChapter::title)
                .filter(Objects::nonNull)
                .filter(title -> !title.isBlank())
                .findFirst()
                .orElse(novel.id().value());
    }

    private static String stringExtension(
            Map<String, Object> extensions,
            String key,
            String fallback
    ) {
        Object value = extensions.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private static List<String> stringListExtension(
            Map<String, Object> extensions,
            String key
    ) {
        Object value = extensions.get(key);
        if (!(value instanceof List<?> entries)) {
            return List.of();
        }
        return entries.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private static int intExtension(Map<String, Object> extensions, String key) {
        Object value = extensions.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }
}

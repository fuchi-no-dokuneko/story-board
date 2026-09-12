package dev.storyblock.application;

import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.RevisionRef;
import java.util.Map;

final class NovelCatalogServiceDescribe {
    static NovelCatalogEntry describe(
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
                NovelCatalogServiceTitle.title(novel),
                NovelCatalogServiceStringExtension.stringExtension(extensions, "language", "und"),
                head.revisionId(),
                head.sequence(),
                head.contentHash(),
                manifest.createdAt(),
                novel.chapters().size(),
                sceneCount,
                manifest.liveBlocks().size(),
                HanText.count(text),
                HanText.sha256(text),
                NovelCatalogServiceStringListExtension.stringListExtension(extensions, "main-characters"),
                NovelCatalogServiceIntExtension.intExtension(extensions, "zombie-count"),
                NovelCatalogServiceIntExtension.intExtension(extensions, "tnt-cannon-count"),
                Boolean.TRUE.equals(extensions.get("agent-write-registered"))
        );
    }
}

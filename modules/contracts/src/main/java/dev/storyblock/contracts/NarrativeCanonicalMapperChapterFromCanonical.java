package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import java.util.List;
import java.util.Map;

final class NarrativeCanonicalMapperChapterFromCanonical {
    static NarrativeChapter chapterFromCanonical(Map<String, Object> chapter) {
        Ids.ChapterId chapterId = new Ids.ChapterId(NarrativeCanonicalMapperRequireString.requireString(chapter, "id"));
        List<NarrativeScene> scenes = NarrativeCanonicalMapperRequireList.requireList(chapter.get("scenes"), "chapter.scenes").stream()
                .map(value -> NarrativeCanonicalMapperSceneFromCanonical.sceneFromCanonical(chapterId, NarrativeCanonicalMapper.requireMap(value, "scene")))
                .toList();
        return new NarrativeChapter(
                chapterId,
                new OrderKey(NarrativeCanonicalMapperRequireString.requireString(chapter, "order_key")),
                NarrativeCanonicalMapperOptionalString.optionalString(chapter, "title"),
                scenes,
                NarrativeCanonicalMapperOptionalMap.optionalMap(chapter.get("extensions"), "chapter.extensions")
        );
    }
}

package dev.storyblock.contracts;

import dev.storyblock.domain.NarrativeChapter;
import java.util.LinkedHashMap;
import java.util.Map;

final class NarrativeCanonicalMapperChapterToCanonical {
    static Map<String, Object> chapterToCanonical(NarrativeChapter chapter) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", chapter.id().value());
        result.put("order_key", chapter.orderKey().value());
        NarrativeCanonicalMapperPutOptional.putOptional(result, "title", chapter.title());
        result.put("scenes", chapter.scenes().stream()
                .map(NarrativeCanonicalMapperSceneToCanonical::sceneToCanonical)
                .toList());
        NarrativeCanonicalMapperPutExtensions.putExtensions(result, chapter.extensions());
        return result;
    }
}

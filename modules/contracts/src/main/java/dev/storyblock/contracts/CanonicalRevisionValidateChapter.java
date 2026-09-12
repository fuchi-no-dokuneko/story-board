package dev.storyblock.contracts;

import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.StableIds;
import java.util.List;
import java.util.Map;
import static dev.storyblock.contracts.CanonicalRevisionFields.CHAPTER_REQUIRED;
import static dev.storyblock.contracts.CanonicalRevisionFields.CHAPTER_OPTIONAL;

final class CanonicalRevisionValidateChapter {
    static void validateChapter(Map<String, Object> chapter, int chapterIndex) {
        String path = "chapter[" + chapterIndex + "]";
        CanonicalRevisionValidateKeys.validateKeys(chapter, CHAPTER_REQUIRED, CHAPTER_OPTIONAL, path);
        StableIds.require(CanonicalRevisionRequireString.requireString(chapter, "id", path), "ch");
        new OrderKey(CanonicalRevisionRequireString.requireString(chapter, "order_key", path));
        CanonicalRevisionOptionalString.optionalString(chapter, "title", path);
        CanonicalRevisionValidateExtensions.validateExtensions(chapter.get("extensions"), path + ".extensions");

        List<Object> scenes = CanonicalRevisionRequireList.requireList(chapter, "scenes", path);
        for (int index = 0; index < scenes.size(); index++) {
            CanonicalRevisionValidateScene.validateScene(CanonicalRevision.requireMap(scenes.get(index), path + ".scene[" + index + "]"), path, index);
        }
    }
}

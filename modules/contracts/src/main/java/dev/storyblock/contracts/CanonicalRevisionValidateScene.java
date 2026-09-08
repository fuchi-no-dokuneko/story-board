package dev.storyblock.contracts;

import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.StableIds;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.contracts.CanonicalRevision.INITIAL_META_FIELDS;
import static dev.storyblock.contracts.CanonicalRevision.SCENE_REQUIRED;
import static dev.storyblock.contracts.CanonicalRevision.TRANSITION_MODES;
import static dev.storyblock.contracts.CanonicalRevision.SCENE_OPTIONAL;

final class CanonicalRevisionValidateScene {
    static void validateScene(
            Map<String, Object> scene,
            String chapterPath,
            int sceneIndex
    ) {
        String path = chapterPath + ".scene[" + sceneIndex + "]";
        CanonicalRevisionValidateKeys.validateKeys(scene, SCENE_REQUIRED, SCENE_OPTIONAL, path);
        StableIds.require(CanonicalRevisionRequireString.requireString(scene, "id", path), "scn");
        StableIds.require(CanonicalRevisionRequireString.requireString(scene, "chapter_id", path), "ch");
        new OrderKey(CanonicalRevisionRequireString.requireString(scene, "order_key", path));
        CanonicalRevisionRequireExactStringInSet.requireExactStringInSet(scene, "transition_mode", TRANSITION_MODES, path);
        CanonicalRevisionOptionalString.optionalString(scene, "title", path);
        CanonicalRevisionValidateExtensions.validateExtensions(scene.get("extensions"), path + ".extensions");
        if (scene.containsKey("initial_meta")) {
            Map<String, Object> initialMeta = CanonicalRevision.requireMap(scene.get("initial_meta"), path + ".initial_meta");
            CanonicalRevisionValidateKeys.validateKeys(initialMeta, Set.of(), INITIAL_META_FIELDS, path + ".initial_meta");
        }

        List<Object> blocks = CanonicalRevisionRequireList.requireList(scene, "blocks", path);
        for (int index = 0; index < blocks.size(); index++) {
            CanonicalRevisionValidateBlock.validateBlock(CanonicalRevision.requireMap(blocks.get(index), path + ".block[" + index + "]"), path, index);
        }
    }
}

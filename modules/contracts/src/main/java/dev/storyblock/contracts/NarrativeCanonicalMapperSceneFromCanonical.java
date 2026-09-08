package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import java.util.List;
import java.util.Map;

final class NarrativeCanonicalMapperSceneFromCanonical {
    static NarrativeScene sceneFromCanonical(
            Ids.ChapterId parentChapterId,
            Map<String, Object> scene
    ) {
        Ids.ChapterId declaredChapterId = new Ids.ChapterId(NarrativeCanonicalMapperRequireString.requireString(scene, "chapter_id"));
        if (!declaredChapterId.equals(parentChapterId)) {
            throw new IllegalArgumentException("Scene chapter_id does not match its canonical parent");
        }
        List<NarrativeBlock> blocks = NarrativeCanonicalMapperRequireList.requireList(scene.get("blocks"), "scene.blocks").stream()
                .map(value -> NarrativeCanonicalMapperBlockFromCanonical.blockFromCanonical(NarrativeCanonicalMapper.requireMap(value, "block")))
                .toList();
        return new NarrativeScene(
                new Ids.SceneId(NarrativeCanonicalMapperRequireString.requireString(scene, "id")),
                declaredChapterId,
                new OrderKey(NarrativeCanonicalMapperRequireString.requireString(scene, "order_key")),
                NarrativeCanonicalMapperOptionalString.optionalString(scene, "title"),
                TransitionMode.fromCanonicalName(NarrativeCanonicalMapperRequireString.requireString(scene, "transition_mode")),
                scene.containsKey("initial_meta")
                        ? new SceneSeed(NarrativeCanonicalMapper.requireMap(scene.get("initial_meta"), "scene.initial_meta"))
                        : null,
                blocks,
                NarrativeCanonicalMapperOptionalMap.optionalMap(scene.get("extensions"), "scene.extensions")
        );
    }
}

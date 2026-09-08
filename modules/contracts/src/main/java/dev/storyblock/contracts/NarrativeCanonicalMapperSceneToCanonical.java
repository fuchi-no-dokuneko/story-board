package dev.storyblock.contracts;

import dev.storyblock.domain.NarrativeScene;
import java.util.LinkedHashMap;
import java.util.Map;

final class NarrativeCanonicalMapperSceneToCanonical {
    static Map<String, Object> sceneToCanonical(NarrativeScene scene) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", scene.id().value());
        result.put("chapter_id", scene.chapterId().value());
        result.put("order_key", scene.orderKey().value());
        NarrativeCanonicalMapperPutOptional.putOptional(result, "title", scene.title());
        result.put("transition_mode", scene.transitionMode().canonicalName());
        if (scene.initialMeta() != null) {
            result.put("initial_meta", scene.initialMeta().fields());
        }
        result.put("blocks", scene.blocks().stream()
                .map(NarrativeCanonicalMapperBlockToCanonical::blockToCanonical)
                .toList());
        NarrativeCanonicalMapperPutExtensions.putExtensions(result, scene.extensions());
        return result;
    }
}

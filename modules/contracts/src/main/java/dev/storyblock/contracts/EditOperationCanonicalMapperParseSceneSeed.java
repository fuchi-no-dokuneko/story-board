package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.SceneSeed;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseSceneSeed {
    static EditOperation parseSceneSeed(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                payload,
                Set.of("scene_id", "expected_boundary", "initial_meta"),
                "set_scene_initial_meta.payload"
        );
        return new EditOperation.SetSceneInitialMeta(
                context,
                new Ids.SceneId(EditOperationCanonicalMapperString.string(payload, "scene_id", "set_scene_initial_meta.payload")),
                EditOperationCanonicalMapperParseBoundary.parseBoundary(EditOperationCanonicalMapper.object(payload.get("expected_boundary"), "expected_boundary")),
                new SceneSeed(EditOperationCanonicalMapper.object(payload.get("initial_meta"), "initial_meta"))
        );
    }
}

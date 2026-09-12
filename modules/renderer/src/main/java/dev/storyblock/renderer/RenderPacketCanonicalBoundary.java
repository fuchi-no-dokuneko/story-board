package dev.storyblock.renderer;

import dev.storyblock.domain.DerivedSceneBoundary;
import java.util.Map;

final class RenderPacketCanonicalBoundary {
    static Map<String, Object> canonicalBoundary(DerivedSceneBoundary boundary) {
        return Map.of(
                "scene_id", boundary.sceneId().value(),
                "state_in", boundary.stateIn(),
                "state_out", boundary.stateOut()
        );
    }
}

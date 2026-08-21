package dev.storyblock.domain;

import java.util.Map;
import java.util.Objects;

public record DerivedSceneBoundary(
        Ids.SceneId sceneId,
        Map<String, Object> stateIn,
        Map<String, Object> stateOut
) {
    public DerivedSceneBoundary {
        Objects.requireNonNull(sceneId, "sceneId");
        stateIn = CanonicalValues.freezeMap(stateIn, "derived_boundary.state_in");
        stateOut = CanonicalValues.freezeMap(stateOut, "derived_boundary.state_out");
    }
}

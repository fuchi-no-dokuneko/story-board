package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.SceneSeed;
import java.util.Objects;

public record SceneSeedChange(
        Ids.SceneId sceneId,
        SceneSeed oldSeed,
        SceneSeed newSeed
) {
    public SceneSeedChange {
        Objects.requireNonNull(sceneId, "sceneId");
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import java.util.Objects;

public record BlockChange(
        Type type,
        Ids.BlockId blockId,
        Ids.BlockVersionId oldVersionId,
        Ids.BlockVersionId newVersionId,
        Ids.SceneId oldSceneId,
        Ids.SceneId newSceneId,
        Integer oldIndex,
        Integer newIndex,
        String oldText,
        String newText
) {
    public enum Type {
        ADDED,
        DELETED,
        MODIFIED,
        MOVED
    }

    public BlockChange {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(blockId, "blockId");
    }
}

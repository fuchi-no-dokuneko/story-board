package dev.storyblock.domain;

import java.util.Objects;

public record InsertionPoint(
        Ids.SceneId sceneId,
        Ids.BlockId anchorBlockId,
        Position position
) {
    public enum Position {
        START,
        BEFORE,
        AFTER,
        END
    }

    public InsertionPoint {
        Objects.requireNonNull(sceneId, "sceneId");
        Objects.requireNonNull(position, "position");
        boolean needsAnchor = position == Position.BEFORE || position == Position.AFTER;
        if (needsAnchor != (anchorBlockId != null)) {
            throw new IllegalArgumentException("BEFORE/AFTER require an anchor; START/END prohibit one");
        }
    }

    public static InsertionPoint startOf(Ids.SceneId sceneId) {
        return new InsertionPoint(sceneId, null, Position.START);
    }

    public static InsertionPoint endOf(Ids.SceneId sceneId) {
        return new InsertionPoint(sceneId, null, Position.END);
    }

    public static InsertionPoint before(Ids.SceneId sceneId, Ids.BlockId blockId) {
        return new InsertionPoint(sceneId, blockId, Position.BEFORE);
    }

    public static InsertionPoint after(Ids.SceneId sceneId, Ids.BlockId blockId) {
        return new InsertionPoint(sceneId, blockId, Position.AFTER);
    }
}

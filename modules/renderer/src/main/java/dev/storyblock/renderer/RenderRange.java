package dev.storyblock.renderer;

import dev.storyblock.domain.Ids;

public record RenderRange(Ids.BlockId fromBlockId, Ids.BlockId toBlockId) {
    public RenderRange {
        if ((fromBlockId == null) != (toBlockId == null)) {
            throw new IllegalArgumentException("Render range must have both endpoints or neither");
        }
    }

    public static RenderRange all() {
        return new RenderRange(null, null);
    }

    public static RenderRange inclusive(Ids.BlockId fromBlockId, Ids.BlockId toBlockId) {
        return new RenderRange(fromBlockId, toBlockId);
    }

    public boolean isAll() {
        return fromBlockId == null;
    }
}

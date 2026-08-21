package dev.storyblock.storage;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import java.util.Objects;

public record BlockTombstone(
        Ids.NovelId novelId,
        Ids.OperationId operationId,
        Ids.RevisionId deletedInRevisionId,
        Ids.SceneId sourceSceneId,
        NarrativeBlock block
) {
    public BlockTombstone {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(deletedInRevisionId, "deletedInRevisionId");
        Objects.requireNonNull(sourceSceneId, "sourceSceneId");
        Objects.requireNonNull(block, "block");
    }
}

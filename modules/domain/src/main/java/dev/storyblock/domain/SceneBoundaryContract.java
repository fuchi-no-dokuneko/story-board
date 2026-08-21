package dev.storyblock.domain;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record SceneBoundaryContract(
        Ids.SceneId sceneId,
        Ids.BlockId firstBlockId,
        Ids.BlockId lastBlockId,
        String expectedSequenceHash
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public SceneBoundaryContract {
        Objects.requireNonNull(sceneId, "sceneId");
        if ((firstBlockId == null) != (lastBlockId == null)) {
            throw new IllegalArgumentException("Scene boundary must have both endpoint IDs or neither");
        }
        if (expectedSequenceHash == null || !SHA_256.matcher(expectedSequenceHash).matches()) {
            throw new IllegalArgumentException("Expected scene sequence hash must be lowercase SHA-256");
        }
    }

    public static SceneBoundaryContract capture(NarrativeScene scene) {
        List<NarrativeBlock> blocks = scene.blocks();
        return new SceneBoundaryContract(
                scene.id(),
                blocks.isEmpty() ? null : blocks.getFirst().id(),
                blocks.isEmpty() ? null : blocks.getLast().id(),
                BlockSequenceHash.ofBlocks(blocks)
        );
    }
}

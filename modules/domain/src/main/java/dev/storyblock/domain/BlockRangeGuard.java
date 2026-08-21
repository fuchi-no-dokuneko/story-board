package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record BlockRangeGuard(
        Ids.SceneId sceneId,
        List<BlockVersionRef> expectedBlocks,
        Ids.BlockId expectedPreviousBlockId,
        Ids.BlockId expectedNextBlockId,
        String expectedRangeHash
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public BlockRangeGuard {
        Objects.requireNonNull(sceneId, "sceneId");
        expectedBlocks = List.copyOf(expectedBlocks);
        if (expectedBlocks.isEmpty()) {
            throw new IllegalArgumentException("A guarded block range cannot be empty");
        }
        Set<Ids.BlockId> unique = new HashSet<>();
        for (BlockVersionRef block : expectedBlocks) {
            Objects.requireNonNull(block, "expected block");
            if (!unique.add(block.blockId())) {
                throw new IllegalArgumentException("A guarded block range cannot repeat a block ID");
            }
        }
        if (unique.contains(expectedPreviousBlockId) || unique.contains(expectedNextBlockId)) {
            throw new IllegalArgumentException("Range anchors cannot be members of the guarded range");
        }
        if (expectedRangeHash == null || !SHA_256.matcher(expectedRangeHash).matches()) {
            throw new IllegalArgumentException("Expected range hash must be lowercase SHA-256");
        }
    }

    public Ids.BlockId firstBlockId() {
        return expectedBlocks.getFirst().blockId();
    }

    public Ids.BlockId lastBlockId() {
        return expectedBlocks.getLast().blockId();
    }

    public static BlockRangeGuard capture(
            NarrativeScene scene,
            Ids.BlockId firstBlockId,
            Ids.BlockId lastBlockId
    ) {
        int first = indexOf(scene.blocks(), firstBlockId);
        int last = indexOf(scene.blocks(), lastBlockId);
        if (first > last) {
            throw new IllegalArgumentException("First block must precede the last block");
        }
        List<NarrativeBlock> range = List.copyOf(scene.blocks().subList(first, last + 1));
        return new BlockRangeGuard(
                scene.id(),
                range.stream().map(BlockVersionRef::from).toList(),
                first == 0 ? null : scene.blocks().get(first - 1).id(),
                last == scene.blocks().size() - 1 ? null : scene.blocks().get(last + 1).id(),
                BlockSequenceHash.ofBlocks(range)
        );
    }

    private static int indexOf(List<NarrativeBlock> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).id().equals(blockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Scene does not contain block " + blockId.value());
    }
}

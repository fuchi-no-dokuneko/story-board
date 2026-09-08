package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import java.util.List;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorBlockIndex {
    static int blockIndex(List<BlockContext> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).blockId().equals(blockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Revision does not contain detector endpoint " + blockId.value()
        );
    }
}

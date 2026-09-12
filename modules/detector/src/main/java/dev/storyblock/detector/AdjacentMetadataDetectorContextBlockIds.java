package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorContextBlockIds {
    static List<Ids.BlockId> contextBlockIds(List<BlockContext> blocks, int index) {
        List<Ids.BlockId> result = new ArrayList<>(3);
        if (index > 0) {
            result.add(blocks.get(index - 1).blockId());
        }
        result.add(blocks.get(index).blockId());
        if (index + 1 < blocks.size()) {
            result.add(blocks.get(index + 1).blockId());
        }
        return List.copyOf(result);
    }
}

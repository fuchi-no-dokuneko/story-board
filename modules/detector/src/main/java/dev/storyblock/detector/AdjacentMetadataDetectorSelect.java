package dev.storyblock.detector;

import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.renderer.RenderRange;
import java.util.List;
import static dev.storyblock.detector.AdjacentMetadataDetector.Selection;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorSelect {
    static Selection select(
            List<BlockContext> blocks,
            List<NarrativeScene> scenes,
            RenderRange requestedRange
    ) {
        if (blocks.isEmpty()) {
            if (!requestedRange.isAll()) {
                throw new IllegalArgumentException("An empty revision has no detector endpoints");
            }
            return new Selection(0, -1, 0, scenes.size() - 1, true);
        }
        int from = requestedRange.isAll()
                ? 0 : AdjacentMetadataDetectorBlockIndex.blockIndex(blocks, requestedRange.fromBlockId());
        int to = requestedRange.isAll()
                ? blocks.size() - 1 : AdjacentMetadataDetectorBlockIndex.blockIndex(blocks, requestedRange.toBlockId());
        if (from > to) {
            throw new IllegalArgumentException("Detector range endpoints are reversed");
        }
        return new Selection(
                from,
                to,
                blocks.get(from).sceneIndex(),
                blocks.get(to).sceneIndex(),
                requestedRange.isAll()
        );
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.StyleAnalysisSnapshot;
import dev.storyblock.style.StyleAnalysisBlock;
import java.util.ArrayList;
import java.util.List;

final class StyleAnalysisServiceSelectBlocks {
    static List<StyleAnalysisBlock> selectBlocks(
            StoredRevision stored,
            Ids.BlockId fromBlockId,
            Ids.BlockId toBlockId
    ) {
        List<StyleAnalysisBlock> all = new ArrayList<>();
        for (NarrativeChapter chapter : stored.manifest().novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                scene.blocks().forEach(block -> all.add(
                        StyleAnalysisBlock.from(scene, block)
                ));
            }
        }
        int first = fromBlockId == null ? 0 : StyleAnalysisServiceIndexOf.indexOf(all, fromBlockId);
        int last = toBlockId == null ? all.size() - 1 : StyleAnalysisServiceIndexOf.indexOf(all, toBlockId);
        if (first < 0 || last < first) {
            throw new IllegalArgumentException("Style analysis block range is invalid");
        }
        List<StyleAnalysisBlock> selected = List.copyOf(all.subList(first, last + 1));
        if (selected.size() > StyleAnalysisSnapshot.MAX_BLOCKS) {
            throw new IllegalArgumentException(
                    "Style analysis range exceeds the 1000-block limit"
            );
        }
        return selected;
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisBlock;
import java.util.List;

final class StyleAnalysisServiceIndexOf {
    static int indexOf(List<StyleAnalysisBlock> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).block().id().equals(blockId)) {
                return index;
            }
        }
        return -1;
    }
}

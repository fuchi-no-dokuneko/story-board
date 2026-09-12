package dev.storyblock.application;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorRebalance {
    static List<NarrativeBlock> rebalance(List<NarrativeBlock> blocks) {
        List<NarrativeBlock> result = new ArrayList<>(blocks.size());
        for (int index = 0; index < blocks.size(); index++) {
            result.add(blocks.get(index).moveTo(OrderKey.rebalanced(index, blocks.size())));
        }
        return List.copyOf(result);
    }
}

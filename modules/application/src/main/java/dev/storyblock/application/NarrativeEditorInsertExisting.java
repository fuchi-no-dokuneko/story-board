package dev.storyblock.application;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorInsertExisting {
    static List<NarrativeBlock> insertExisting(
            List<NarrativeBlock> retained,
            int insertionIndex,
            List<NarrativeBlock> moving
    ) {
        try {
            OrderKey left = insertionIndex == 0 ? null : retained.get(insertionIndex - 1).orderKey();
            OrderKey right = insertionIndex == retained.size() ? null : retained.get(insertionIndex).orderKey();
            List<NarrativeBlock> inserted = new ArrayList<>(retained);
            int offset = 0;
            for (NarrativeBlock block : moving) {
                OrderKey key = OrderKey.between(left, right);
                inserted.add(insertionIndex + offset, block.moveTo(key));
                left = key;
                offset++;
            }
            return List.copyOf(inserted);
        } catch (IllegalStateException exhaustedOrderSpace) {
            List<NarrativeBlock> sequence = new ArrayList<>(retained);
            sequence.addAll(insertionIndex, moving);
            return NarrativeEditorRebalance.rebalance(sequence);
        }
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorInsertDrafts {
    static List<NarrativeBlock> insertDrafts(
            List<NarrativeBlock> retained,
            int insertionIndex,
            List<BlockDraft> drafts,
            Ids.OperationId operationId
    ) {
        if (drafts.isEmpty()) {
            return List.copyOf(retained);
        }
        try {
            OrderKey left = insertionIndex == 0 ? null : retained.get(insertionIndex - 1).orderKey();
            OrderKey right = insertionIndex == retained.size() ? null : retained.get(insertionIndex).orderKey();
            List<NarrativeBlock> inserted = new ArrayList<>(retained);
            int offset = 0;
            for (BlockDraft draft : drafts) {
                OrderKey key = OrderKey.between(left, right);
                inserted.add(insertionIndex + offset, draft.materialize(
                        key, NarrativeEditorDerivedVersion.derivedVersion(operationId, draft.id())
                ));
                left = key;
                offset++;
            }
            return List.copyOf(inserted);
        } catch (IllegalStateException exhaustedOrderSpace) {
            return NarrativeEditorRebalanceWithDrafts.rebalanceWithDrafts(retained, insertionIndex, drafts, operationId);
        }
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorRebalanceWithDrafts {
    static List<NarrativeBlock> rebalanceWithDrafts(
            List<NarrativeBlock> retained,
            int insertionIndex,
            List<BlockDraft> drafts,
            Ids.OperationId operationId
    ) {
        int total = retained.size() + drafts.size();
        List<NarrativeBlock> result = new ArrayList<>(total);
        int retainedIndex = 0;
        int draftIndex = 0;
        for (int index = 0; index < total; index++) {
            OrderKey key = OrderKey.rebalanced(index, total);
            if (index >= insertionIndex && draftIndex < drafts.size()) {
                BlockDraft draft = drafts.get(draftIndex++);
                result.add(draft.materialize(key, NarrativeEditorDerivedVersion.derivedVersion(operationId, draft.id())));
            } else {
                result.add(retained.get(retainedIndex++).moveTo(key));
            }
        }
        return List.copyOf(result);
    }
}

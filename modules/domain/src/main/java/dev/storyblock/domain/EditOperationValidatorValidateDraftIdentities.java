package dev.storyblock.domain;

import java.util.List;
import java.util.Set;

final class EditOperationValidatorValidateDraftIdentities {
    static void validateDraftIdentities(
            RevisionManifest base,
            List<BlockDraft> drafts,
            Set<Ids.BlockId> replaceableIds
    ) {
        Set<Ids.BlockId> liveIds = base.selectedBlockVersions().keySet();
        for (BlockDraft draft : drafts) {
            if (liveIds.contains(draft.id()) && !replaceableIds.contains(draft.id())) {
                throw new EditInvariantException(
                        EditInvariantException.Code.DUPLICATE_BLOCK_ID,
                        "Draft reuses a live block ID outside the replaced range: " + draft.id().value()
                );
            }
        }
    }
}

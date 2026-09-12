package dev.storyblock.domain;

import java.util.Set;

final class EditOperationValidatorValidateInsert {
    static void validateInsert(RevisionManifest base, EditOperation.InsertBlocks insert) {
        NarrativeScene scene = base.requireScene(insert.insertionPoint().sceneId());
        EditOperationValidator.insertionIndex(scene, insert.insertionPoint());
        EditOperationValidatorValidateDraftIdentities.validateDraftIdentities(base, insert.blocks(), Set.of());
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;

final class NarrativeEditorApplyInsert {
    static NarrativeNovel applyInsert(
            RevisionManifest base,
            EditOperation.InsertBlocks operation
    ) {
        NarrativeScene scene = base.requireScene(operation.insertionPoint().sceneId());
        int index = EditOperationValidator.insertionIndex(scene, operation.insertionPoint());
        NarrativeScene updated = scene.withBlocks(NarrativeEditorInsertDrafts.insertDrafts(
                scene.blocks(), index, operation.blocks(), operation.context().operationId()
        ));
        return NarrativeEditorReplaceScene.replaceScene(base.novel(), updated);
    }
}

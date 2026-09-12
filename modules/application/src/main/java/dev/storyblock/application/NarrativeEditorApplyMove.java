package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorApplyMove {
    static NarrativeNovel applyMove(
            RevisionManifest base,
            EditOperation.MoveBlockRange operation
    ) {
        EditOperationValidator.RangeLocation source = EditOperationValidator.validateRange(
                base, operation.range()
        );
        List<NarrativeBlock> moving = source.blocks();

        if (source.scene().id().equals(operation.destination().sceneId())) {
            List<NarrativeBlock> retained = new ArrayList<>(source.scene().blocks());
            retained.subList(source.firstIndex(), source.lastIndex() + 1).clear();
            int destinationIndex = NarrativeEditorInsertionIndexAfterRemoval.insertionIndexAfterRemoval(retained, operation.destination());
            NarrativeScene updated = source.scene().withBlocks(
                    NarrativeEditorInsertExisting.insertExisting(retained, destinationIndex, moving)
            );
            return NarrativeEditorReplaceScene.replaceScene(base.novel(), updated);
        }

        NarrativeScene destination = base.requireScene(operation.destination().sceneId());
        List<NarrativeBlock> sourceBlocks = new ArrayList<>(source.scene().blocks());
        sourceBlocks.subList(source.firstIndex(), source.lastIndex() + 1).clear();
        int destinationIndex = EditOperationValidator.insertionIndex(
                destination, operation.destination()
        );
        NarrativeScene updatedSource = source.scene().withBlocks(sourceBlocks);
        NarrativeScene updatedDestination = destination.withBlocks(
                NarrativeEditorInsertExisting.insertExisting(destination.blocks(), destinationIndex, moving)
        );
        return NarrativeEditorReplaceScene.replaceScene(NarrativeEditorReplaceScene.replaceScene(base.novel(), updatedSource), updatedDestination);
    }
}

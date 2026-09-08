package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorApplyCorrection {
    static NarrativeNovel applyCorrection(
            RevisionManifest base,
            EditOperation.CorrectBlockMeta operation
    ) {
        NarrativeScene scene = base.requireScene(operation.sceneId());
        List<NarrativeBlock> blocks = new ArrayList<>(scene.blocks());
        int index = NarrativeEditorIndexOf.indexOf(blocks, operation.block().blockId());
        NarrativeBlock current = blocks.get(index);
        blocks.set(index, current.revise(
                current.text(),
                operation.correctedMetadata(),
                current.extensions(),
                NarrativeEditorDerivedVersion.derivedVersion(operation.context().operationId(), current.id())
        ));
        return NarrativeEditorReplaceScene.replaceScene(base.novel(), scene.withBlocks(blocks));
    }
}

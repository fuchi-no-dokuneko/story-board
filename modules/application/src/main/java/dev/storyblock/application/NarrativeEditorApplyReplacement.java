package dev.storyblock.application;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorApplyReplacement {
    static NarrativeNovel applyReplacement(
            RevisionManifest base,
            Ids.OperationId operationId,
            BlockRangeGuard guard,
            List<BlockDraft> replacements
    ) {
        EditOperationValidator.RangeLocation range = EditOperationValidator.validateRange(base, guard);
        List<NarrativeBlock> retained = new ArrayList<>(range.scene().blocks());
        retained.subList(range.firstIndex(), range.lastIndex() + 1).clear();
        NarrativeScene updated = range.scene().withBlocks(
                NarrativeEditorInsertDrafts.insertDrafts(retained, range.firstIndex(), replacements, operationId)
        );
        return NarrativeEditorReplaceScene.replaceScene(base.novel(), updated);
    }
}

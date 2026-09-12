package dev.storyblock.application;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.RevisionManifest;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

final class NarrativeEditorApplyAction {
    static RevisionManifest apply(NarrativeEditor self, RevisionManifest base, EditOperation operation, Ids.RevisionId newRevisionId, Instant createdAt)  {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(newRevisionId, "newRevisionId");
        Objects.requireNonNull(createdAt, "createdAt");
        String actualHeadHash = NarrativeCanonicalMapper.toCanonical(base).contentHash();
        EditOperationValidator.validate(base, actualHeadHash, operation);

        NarrativeNovel updated = switch (operation) {
            case EditOperation.InsertBlocks insert -> NarrativeEditorApplyInsert.applyInsert(base, insert);
            case EditOperation.ReplaceBlockRange replace -> NarrativeEditorApplyReplacement.applyReplacement(
                    base, replace.context().operationId(), replace.range(), replace.newBlocks()
            );
            case EditOperation.DeleteBlockRange delete -> NarrativeEditorApplyReplacement.applyReplacement(
                    base, delete.context().operationId(), delete.range(), List.of()
            );
            case EditOperation.SplitBlock split -> NarrativeEditorApplyReplacement.applyReplacement(
                    base, split.context().operationId(), split.block(), split.newBlocks()
            );
            case EditOperation.MergeBlocks merge -> NarrativeEditorApplyReplacement.applyReplacement(
                    base, merge.context().operationId(), merge.range(), List.of(merge.newBlock())
            );
            case EditOperation.ExtendBlock extend -> NarrativeEditorApplyReplacement.applyReplacement(
                    base, extend.context().operationId(), extend.block(), List.of(extend.replacement())
            );
            case EditOperation.MoveBlockRange move -> NarrativeEditorApplyMove.applyMove(base, move);
            case EditOperation.CorrectBlockMeta correction -> NarrativeEditorApplyCorrection.applyCorrection(base, correction);
            case EditOperation.SetSceneInitialMeta sceneSeed -> NarrativeEditorApplySceneSeed.applySceneSeed(base, sceneSeed);
            case EditOperation.RestoreRevisionContent restore -> self.restoreContent(base, restore);
        };

        return new RevisionManifest(newRevisionId, base.id(), createdAt, updated);
    }
}

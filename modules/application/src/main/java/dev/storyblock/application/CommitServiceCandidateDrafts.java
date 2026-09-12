package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.BlockDraft;
import java.util.List;

final class CommitServiceCandidateDrafts {
    static List<BlockDraft> candidateDrafts(EditOperation operation) {
        return switch (operation) {
            case EditOperation.InsertBlocks insert -> insert.blocks();
            case EditOperation.ReplaceBlockRange replace -> replace.newBlocks();
            case EditOperation.SplitBlock split -> split.newBlocks();
            case EditOperation.MergeBlocks merge -> List.of(merge.newBlock());
            case EditOperation.ExtendBlock extend -> List.of(extend.replacement());
            case EditOperation.DeleteBlockRange ignored -> List.of();
            case EditOperation.MoveBlockRange ignored -> List.of();
            case EditOperation.CorrectBlockMeta ignored -> List.of();
            case EditOperation.SetSceneInitialMeta ignored -> List.of();
            case EditOperation.RestoreRevisionContent ignored -> List.of();
        };
    }
}

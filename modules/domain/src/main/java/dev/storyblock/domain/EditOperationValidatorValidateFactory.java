package dev.storyblock.domain;

import java.util.List;
import static dev.storyblock.domain.EditOperationValidator.RangeLocation;

final class EditOperationValidatorValidateFactory {
    static void validate(RevisionManifest base, String actualHeadHash, EditOperation operation)  {
        EditOperationValidatorValidateContext.validateContext(base, actualHeadHash, operation.context());
        switch (operation) {
            case EditOperation.InsertBlocks insert -> EditOperationValidatorValidateInsert.validateInsert(base, insert);
            case EditOperation.ReplaceBlockRange replace -> {
                EditOperationValidator.validateRange(base, replace.range());
                EditOperationValidatorValidateDraftIdentities.validateDraftIdentities(base, replace.newBlocks(), EditOperationValidatorRangeIds.rangeIds(replace.range()));
            }
            case EditOperation.DeleteBlockRange delete -> EditOperationValidator.validateRange(base, delete.range());
            case EditOperation.SplitBlock split -> EditOperationValidatorValidateSplit.validateSplit(base, split);
            case EditOperation.MergeBlocks merge -> {
                RangeLocation range = EditOperationValidator.validateRange(base, merge.range());
                if (range.blocks().stream().anyMatch(block -> block.image().isPresent())
                        || merge.newBlock().image().isPresent()) {
                    throw EditOperationValidatorInvalid.invalid("merge_blocks does not support image blocks");
                }
                EditOperationValidatorValidateDraftIdentities.validateDraftIdentities(base, List.of(merge.newBlock()), EditOperationValidatorRangeIds.rangeIds(merge.range()));
            }
            case EditOperation.ExtendBlock extend -> EditOperationValidatorValidateExtend.validateExtend(base, extend);
            case EditOperation.MoveBlockRange move -> EditOperationValidatorValidateMove.validateMove(base, move);
            case EditOperation.CorrectBlockMeta correction -> EditOperationValidatorValidateCorrection.validateCorrection(base, correction);
            case EditOperation.SetSceneInitialMeta sceneSeed -> EditOperationValidatorValidateSceneSeed.validateSceneSeed(base, sceneSeed);
            case EditOperation.RestoreRevisionContent ignored -> {
                // The target revision and its hash are checked by the application revision lookup.
            }
        }
    }
}

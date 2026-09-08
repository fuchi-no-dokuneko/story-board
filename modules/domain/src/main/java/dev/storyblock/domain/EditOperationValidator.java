package dev.storyblock.domain;

import java.util.List;

public final class EditOperationValidator {
    private EditOperationValidator() {
    }

    public static void validate(
            RevisionManifest base,
            String actualHeadHash,
            EditOperation operation
    ) {
        EditOperationValidatorValidateContext.validateContext(base, actualHeadHash, operation.context());
        switch (operation) {
            case EditOperation.InsertBlocks insert -> EditOperationValidatorValidateInsert.validateInsert(base, insert);
            case EditOperation.ReplaceBlockRange replace -> {
                validateRange(base, replace.range());
                EditOperationValidatorValidateDraftIdentities.validateDraftIdentities(base, replace.newBlocks(), EditOperationValidatorRangeIds.rangeIds(replace.range()));
            }
            case EditOperation.DeleteBlockRange delete -> validateRange(base, delete.range());
            case EditOperation.SplitBlock split -> EditOperationValidatorValidateSplit.validateSplit(base, split);
            case EditOperation.MergeBlocks merge -> {
                RangeLocation range = validateRange(base, merge.range());
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

    public static RangeLocation validateRange(RevisionManifest base, BlockRangeGuard guard) {
        NarrativeScene scene = base.requireScene(guard.sceneId());
        List<NarrativeBlock> blocks = scene.blocks();
        int first = EditOperationValidatorIndexOf.indexOf(blocks, guard.firstBlockId());
        int last = first + guard.expectedBlocks().size() - 1;
        if (last >= blocks.size() || !blocks.get(last).id().equals(guard.lastBlockId())) {
            throw EditOperationValidatorAdjacency.adjacency("Guarded range is not contiguous in the base revision");
        }

        for (int offset = 0; offset < guard.expectedBlocks().size(); offset++) {
            NarrativeBlock actual = blocks.get(first + offset);
            BlockVersionRef expected = guard.expectedBlocks().get(offset);
            if (!actual.id().equals(expected.blockId())) {
                throw EditOperationValidatorAdjacency.adjacency("Guarded range block order no longer matches the base revision");
            }
            if (!actual.versionId().equals(expected.blockVersionId())) {
                throw new EditInvariantException(
                        EditInvariantException.Code.BLOCK_VERSION_CONFLICT,
                        "Block version changed for " + actual.id().value()
                );
            }
        }

        Ids.BlockId previous = first == 0 ? null : blocks.get(first - 1).id();
        Ids.BlockId next = last == blocks.size() - 1 ? null : blocks.get(last + 1).id();
        if (!java.util.Objects.equals(previous, guard.expectedPreviousBlockId())
                || !java.util.Objects.equals(next, guard.expectedNextBlockId())) {
            throw EditOperationValidatorAdjacency.adjacency("Guarded range anchors no longer match the base revision");
        }

        List<NarrativeBlock> actualRange = blocks.subList(first, last + 1);
        if (!BlockSequenceHash.ofBlocks(actualRange).equals(guard.expectedRangeHash())) {
            throw EditOperationValidatorAdjacency.adjacency("Guarded range hash no longer matches the base revision");
        }
        return new RangeLocation(scene, first, last);
    }

    public static int insertionIndex(NarrativeScene scene, InsertionPoint insertionPoint) {
        if (!scene.id().equals(insertionPoint.sceneId())) {
            throw EditOperationValidatorInvalid.invalid("Insertion point belongs to a different scene");
        }
        return switch (insertionPoint.position()) {
            case START -> 0;
            case END -> scene.blocks().size();
            case BEFORE -> EditOperationValidatorIndexOf.indexOf(scene.blocks(), insertionPoint.anchorBlockId());
            case AFTER -> EditOperationValidatorIndexOf.indexOf(scene.blocks(), insertionPoint.anchorBlockId()) + 1;
        };
    }

    public static void validateBoundary(NarrativeScene scene, SceneBoundaryContract boundary) {
        if (!scene.id().equals(boundary.sceneId())) {
            throw EditOperationValidatorAdjacency.adjacency("Scene boundary identifies a different scene");
        }
        Ids.BlockId first = scene.blocks().isEmpty() ? null : scene.blocks().getFirst().id();
        Ids.BlockId last = scene.blocks().isEmpty() ? null : scene.blocks().getLast().id();
        if (!java.util.Objects.equals(first, boundary.firstBlockId())
                || !java.util.Objects.equals(last, boundary.lastBlockId())
                || !BlockSequenceHash.ofBlocks(scene.blocks()).equals(boundary.expectedSequenceHash())) {
            throw EditOperationValidatorAdjacency.adjacency("Scene boundary contract is stale");
        }
    }

    public record RangeLocation(NarrativeScene scene, int firstIndex, int lastIndex) {
        public List<NarrativeBlock> blocks() {
            return List.copyOf(scene.blocks().subList(firstIndex, lastIndex + 1));
        }
    }
}

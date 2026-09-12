package dev.storyblock.domain;

import java.util.List;
import static dev.storyblock.domain.EditOperationValidator.RangeLocation;

final class EditOperationValidatorValidateRangeFactory {
    static RangeLocation validateRange(RevisionManifest base, BlockRangeGuard guard)  {
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
}

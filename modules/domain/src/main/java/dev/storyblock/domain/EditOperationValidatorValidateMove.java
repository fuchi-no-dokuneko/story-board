package dev.storyblock.domain;

import java.util.Set;
import static dev.storyblock.domain.EditOperationValidator.RangeLocation;

final class EditOperationValidatorValidateMove {
    static void validateMove(RevisionManifest base, EditOperation.MoveBlockRange move) {
        RangeLocation source = EditOperationValidator.validateRange(base, move.range());
        NarrativeScene destination = base.requireScene(move.destination().sceneId());
        EditOperationValidator.validateBoundary(source.scene(), move.expectedSourceBoundary());
        EditOperationValidator.validateBoundary(destination, move.expectedDestinationBoundary());
        EditOperationValidator.insertionIndex(destination, move.destination());

        if (source.scene().id().equals(destination.id())) {
            Set<Ids.BlockId> movingIds = EditOperationValidatorRangeIds.rangeIds(move.range());
            if (movingIds.contains(move.destination().anchorBlockId())) {
                throw EditOperationValidatorInvalid.invalid("A moved range cannot use one of its own blocks as destination anchor");
            }
            int insertion = EditOperationValidator.insertionIndex(destination, move.destination());
            if (insertion == source.firstIndex() || insertion == source.lastIndex() + 1) {
                throw EditOperationValidatorInvalid.invalid("move_block_range cannot be a no-op");
            }
        }
    }
}

package dev.storyblock.domain;

import java.util.List;

public final class EditOperationValidator {
    EditOperationValidator() {
    }

    public static void validate(
            RevisionManifest base,
            String actualHeadHash,
            EditOperation operation
    ) {
        EditOperationValidatorValidateFactory.validate(base, actualHeadHash, operation);
    }

    public static RangeLocation validateRange(RevisionManifest base, BlockRangeGuard guard) {
        return EditOperationValidatorValidateRangeFactory.validateRange(base, guard);
    }

    public static int insertionIndex(NarrativeScene scene, InsertionPoint insertionPoint) {
        return EditOperationValidatorInsertionIndexFactory.insertionIndex(scene, insertionPoint);
    }

    public static void validateBoundary(NarrativeScene scene, SceneBoundaryContract boundary) {
        EditOperationValidatorValidateBoundaryFactory.validateBoundary(scene, boundary);
    }

    public record RangeLocation(NarrativeScene scene, int firstIndex, int lastIndex) {
        public List<NarrativeBlock> blocks() {
            return List.copyOf(scene.blocks().subList(firstIndex, lastIndex + 1));
        }
    }
}

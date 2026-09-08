package dev.storyblock.domain;



final class EditOperationValidatorValidateContext {
    static void validateContext(
            RevisionManifest base,
            String actualHeadHash,
            EditContext context
    ) {
        if (!base.novel().id().equals(context.novelId())) {
            throw EditOperationValidatorConflict.conflict("Operation novel does not match the loaded revision");
        }
        if (!base.id().equals(context.baseRevisionId())) {
            throw EditOperationValidatorConflict.conflict("Operation base revision does not match the loaded revision");
        }
        if (!context.expectedHeadHash().equals(actualHeadHash)) {
            throw EditOperationValidatorConflict.conflict("Expected head hash does not match the loaded revision");
        }
    }
}

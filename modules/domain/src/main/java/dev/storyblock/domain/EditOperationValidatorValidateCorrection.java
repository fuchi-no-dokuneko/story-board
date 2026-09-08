package dev.storyblock.domain;



final class EditOperationValidatorValidateCorrection {
    static void validateCorrection(
            RevisionManifest base,
            EditOperation.CorrectBlockMeta correction
    ) {
        NarrativeScene scene = base.requireScene(correction.sceneId());
        int index = EditOperationValidatorIndexOf.indexOf(scene.blocks(), correction.block().blockId());
        NarrativeBlock block = scene.blocks().get(index);
        if (!block.versionId().equals(correction.block().blockVersionId())) {
            throw new EditInvariantException(
                    EditInvariantException.Code.BLOCK_VERSION_CONFLICT,
                    "Block version changed for " + block.id().value()
            );
        }
        if (block.metadata().equals(correction.correctedMetadata())) {
            throw EditOperationValidatorInvalid.invalid("correct_block_meta cannot submit unchanged metadata");
        }
    }
}

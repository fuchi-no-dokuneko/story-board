package dev.storyblock.domain;



final class EditOperationValidatorValidateSceneSeed {
    static void validateSceneSeed(
            RevisionManifest base,
            EditOperation.SetSceneInitialMeta operation
    ) {
        NarrativeScene scene = base.requireScene(operation.sceneId());
        EditOperationValidator.validateBoundary(scene, operation.expectedBoundary());
        if (operation.initialMeta().equals(scene.initialMeta())) {
            throw EditOperationValidatorInvalid.invalid("set_scene_initial_meta cannot submit an unchanged scene seed");
        }
    }
}

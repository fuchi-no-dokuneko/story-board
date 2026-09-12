package dev.storyblock.domain;



final class EditOperationValidatorInsertionIndexFactory {
    static int insertionIndex(NarrativeScene scene, InsertionPoint insertionPoint)  {
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
}

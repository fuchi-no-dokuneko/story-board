package dev.storyblock.domain;



final class EditOperationValidatorValidateBoundaryFactory {
    static void validateBoundary(NarrativeScene scene, SceneBoundaryContract boundary)  {
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
}

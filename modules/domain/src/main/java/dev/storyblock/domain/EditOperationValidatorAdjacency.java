package dev.storyblock.domain;



final class EditOperationValidatorAdjacency {
    static EditInvariantException adjacency(String message) {
        return new EditInvariantException(EditInvariantException.Code.INVALID_BLOCK_ADJACENCY, message);
    }
}

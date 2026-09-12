package dev.storyblock.domain;



final class EditOperationValidatorConflict {
    static EditInvariantException conflict(String message) {
        return new EditInvariantException(EditInvariantException.Code.REVISION_CONFLICT, message);
    }
}

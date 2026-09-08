package dev.storyblock.domain;



final class EditOperationValidatorInvalid {
    static EditInvariantException invalid(String message) {
        return new EditInvariantException(EditInvariantException.Code.INVALID_OPERATION, message);
    }
}

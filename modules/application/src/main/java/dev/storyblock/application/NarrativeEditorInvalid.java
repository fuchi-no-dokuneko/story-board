package dev.storyblock.application;

import dev.storyblock.domain.EditInvariantException;

final class NarrativeEditorInvalid {
    static EditInvariantException invalid(String message) {
        return new EditInvariantException(EditInvariantException.Code.INVALID_OPERATION, message);
    }
}

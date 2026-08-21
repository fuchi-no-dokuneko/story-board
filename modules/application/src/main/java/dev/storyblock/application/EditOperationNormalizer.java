package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;

public final class EditOperationNormalizer {
    private EditOperationNormalizer() {
    }

    public static EditOperation normalize(EditOperation operation) {
        if (operation instanceof EditOperation.ExtendBlock extend) {
            return new EditOperation.ReplaceBlockRange(
                    extend.context(),
                    extend.block(),
                    java.util.List.of(extend.replacement())
            );
        }
        return operation;
    }
}

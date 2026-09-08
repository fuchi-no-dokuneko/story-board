package dev.storyblock.validator;

import dev.storyblock.domain.EditInvariantException;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.RevisionManifest;
import java.util.List;
import java.util.Map;

final class DeterministicValidatorValidateOperationAction {
    static ValidationReport validateOperation(DeterministicValidator self, RevisionManifest base, String actualHeadHash, EditOperation operation)  {
        try {
            EditOperationValidator.validate(base, actualHeadHash, operation);
            return ValidationReport.empty();
        } catch (EditInvariantException exception) {
            ValidationCode code = switch (exception.code()) {
                case REVISION_CONFLICT -> ValidationCode.REVISION_CONFLICT;
                case INVALID_BLOCK_ADJACENCY, BLOCK_VERSION_CONFLICT,
                        DUPLICATE_BLOCK_ID, INVALID_OPERATION -> ValidationCode.INVALID_BLOCK_ADJACENCY;
            };
            return DeterministicValidatorErrors.errors(List.of(ValidationIssue.error(
                    code,
                    null,
                    exception.getMessage(),
                    Map.of(
                            "operation_id", operation.context().operationId().value(),
                            "operation_type", operation.type().canonicalName(),
                            "base_revision_id", base.id().value()
                    )
            )));
        } catch (IllegalArgumentException exception) {
            return DeterministicValidatorErrors.errors(List.of(ValidationIssue.error(
                    ValidationCode.INVALID_BLOCK_ADJACENCY,
                    null,
                    exception.getMessage(),
                    Map.of(
                            "operation_id", operation.context().operationId().value(),
                            "operation_type", operation.type().canonicalName(),
                            "base_revision_id", base.id().value()
                    )
            )));
        }
    }
}

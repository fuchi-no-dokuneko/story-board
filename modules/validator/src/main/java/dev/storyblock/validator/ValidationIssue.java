package dev.storyblock.validator;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Objects;

public record ValidationIssue(
        ValidationCode code,
        ValidationSeverity severity,
        Ids.BlockId blockId,
        String message,
        Map<String, Object> details
) {
    public ValidationIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Validation issue message cannot be blank");
        }
        details = CanonicalValues.freezeMap(details, "validation_issue.details");
    }

    public static ValidationIssue error(
            ValidationCode code,
            Ids.BlockId blockId,
            String message,
            Map<String, Object> details
    ) {
        return new ValidationIssue(code, ValidationSeverity.ERROR, blockId, message, details);
    }
}

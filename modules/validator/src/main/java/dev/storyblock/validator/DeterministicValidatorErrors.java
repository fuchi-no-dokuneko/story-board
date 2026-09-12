package dev.storyblock.validator;

import java.util.List;

final class DeterministicValidatorErrors {
    static ValidationReport errors(List<ValidationIssue> issues) {
        return new ValidationReport(issues, List.of());
    }
}

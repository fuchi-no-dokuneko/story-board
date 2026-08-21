package dev.storyblock.validator;

import java.util.ArrayList;
import java.util.List;

public record ValidationReport(
        List<ValidationIssue> violations,
        List<ValidationIssue> warnings
) {
    public ValidationReport {
        violations = List.copyOf(violations);
        warnings = List.copyOf(warnings);
        if (violations.stream().anyMatch(issue -> issue.severity() != ValidationSeverity.ERROR)) {
            throw new IllegalArgumentException("Violation list can only contain errors");
        }
        if (warnings.stream().anyMatch(issue -> issue.severity() != ValidationSeverity.WARNING)) {
            throw new IllegalArgumentException("Warning list can only contain warnings");
        }
    }

    public static ValidationReport empty() {
        return new ValidationReport(List.of(), List.of());
    }

    public boolean committable() {
        return violations.isEmpty();
    }

    public ValidationReport plus(ValidationReport other) {
        List<ValidationIssue> errors = new ArrayList<>(violations);
        errors.addAll(other.violations);
        List<ValidationIssue> notices = new ArrayList<>(warnings);
        notices.addAll(other.warnings);
        return new ValidationReport(errors, notices);
    }
}

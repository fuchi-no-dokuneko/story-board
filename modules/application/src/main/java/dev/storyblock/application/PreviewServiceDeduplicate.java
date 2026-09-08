package dev.storyblock.application;

import dev.storyblock.validator.ValidationIssue;
import dev.storyblock.validator.ValidationReport;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class PreviewServiceDeduplicate {
    static ValidationReport deduplicate(ValidationReport report) {
        List<ValidationIssue> errors = new ArrayList<>(new LinkedHashSet<>(report.violations()));
        List<ValidationIssue> warnings = new ArrayList<>(new LinkedHashSet<>(report.warnings()));
        return new ValidationReport(errors, warnings);
    }
}

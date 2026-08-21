package dev.storyblock.domain;

import java.util.List;

public final class InvalidBlockTextException extends IllegalArgumentException {
    private final TextAnalysis analysis;

    public InvalidBlockTextException(TextAnalysis analysis) {
        super("Invalid narrative block text: " + analysis.violations());
        this.analysis = analysis;
    }

    public TextAnalysis analysis() {
        return analysis;
    }

    public List<BlockTextViolation> violations() {
        return analysis.violations();
    }
}

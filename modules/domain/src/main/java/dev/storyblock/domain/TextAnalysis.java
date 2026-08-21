package dev.storyblock.domain;

import java.util.List;
import java.util.ArrayList;

public record TextAnalysis(
        String parserVersion,
        String normalizationVersion,
        String normalizedText,
        int graphemeCount,
        int sentenceCount,
        boolean complete,
        List<Integer> safeSplitAnchors
) {
    public TextAnalysis {
        safeSplitAnchors = List.copyOf(safeSplitAnchors);
    }

    public List<BlockTextViolation> violations() {
        List<BlockTextViolation> violations = new ArrayList<>();
        if (normalizedText.isBlank()) {
            violations.add(BlockTextViolation.EMPTY);
        } else {
            if (sentenceCount < 1 || sentenceCount > 2) {
                violations.add(BlockTextViolation.INVALID_SENTENCE_COUNT);
            }
            if (!complete) {
                violations.add(BlockTextViolation.INCOMPLETE_SENTENCE);
            }
        }
        if (graphemeCount > UnicodeText.MAX_BLOCK_GRAPHEMES) {
            violations.add(BlockTextViolation.TOO_LONG);
        }
        return List.copyOf(violations);
    }

    public boolean validBlockShape() {
        return violations().isEmpty();
    }
}

package dev.storyblock.validator;

import java.util.List;
import java.util.Map;

final class DeterministicValidatorHasProvenanceEvidence {
    static boolean hasProvenanceEvidence(Object value, String text) {
        return value instanceof Map<?, ?> map
                && map.get("evidence") instanceof List<?> evidence
                && evidence.stream().anyMatch(entry -> entry instanceof Map<?, ?> span
                        && EvidenceSpans.matches(text, span));
    }
}

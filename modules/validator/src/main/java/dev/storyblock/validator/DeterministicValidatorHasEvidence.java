package dev.storyblock.validator;

import java.util.Map;

final class DeterministicValidatorHasEvidence {
    static boolean hasEvidence(Object value, String text) {
        return value instanceof Map<?, ?> map
                && map.get("evidence") instanceof Map<?, ?> evidence
                && EvidenceSpans.matches(text, evidence);
    }
}

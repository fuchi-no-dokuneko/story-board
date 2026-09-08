package dev.storyblock.detector;

import dev.storyblock.validator.EvidenceSpans;
import java.util.Map;

final class AdjacentMetadataDetectorMatchesEvidence {
    static boolean matchesEvidence(String text, Object raw) {
        return raw instanceof Map<?, ?> evidence && EvidenceSpans.matches(text, evidence);
    }
}

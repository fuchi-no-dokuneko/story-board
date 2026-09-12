package dev.storyblock.detector;

import java.util.List;
import java.util.Map;

final class AdjacentMetadataDetectorHasValidEvidence {
    static boolean hasValidEvidence(
            Object observation,
            Map<String, Object> localMetadata,
            String text
    ) {
        if (observation instanceof Map<?, ?> map
                && AdjacentMetadataDetectorMatchesEvidence.matchesEvidence(text, map.get("evidence"))) {
            return true;
        }
        Object provenance = localMetadata.get("provenance");
        if (!(provenance instanceof Map<?, ?> map)
                || !(map.get("evidence") instanceof List<?> entries)) {
            return false;
        }
        return entries.stream().anyMatch(entry -> AdjacentMetadataDetectorMatchesEvidence.matchesEvidence(text, entry));
    }
}

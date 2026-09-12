package dev.storyblock.rewrite.policy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAddMarkers {
    static void addMarkers(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            String text,
            List<String> markers
    ) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            boolean word = marker.chars().allMatch(value -> value < 128);
            int count = RewriteProtectedFactExtractorOccurrences.occurrences(lower, marker, word);
            if (count > 0) {
                RewriteProtectedFactExtractorAdd.add(facts, kind, marker, count);
            }
        }
    }
}

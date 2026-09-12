package dev.storyblock.style;

import java.util.Map;

final class StyleFeatureAnalyzerIncrement {
    static void increment(Map<String, Long> counts, String key) {
        counts.merge(key, 1L, Long::sum);
    }
}

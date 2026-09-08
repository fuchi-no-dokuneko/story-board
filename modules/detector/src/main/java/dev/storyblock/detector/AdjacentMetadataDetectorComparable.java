package dev.storyblock.detector;

import java.util.Map;

final class AdjacentMetadataDetectorComparable {
    static boolean comparable(Object value) {
        return !(value instanceof Map<?, ?> map
                && ("unknown".equals(map.get("mode"))
                || "not_applicable".equals(map.get("mode"))));
    }
}

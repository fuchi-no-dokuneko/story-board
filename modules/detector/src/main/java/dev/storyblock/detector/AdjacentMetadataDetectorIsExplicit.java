package dev.storyblock.detector;

import java.util.Map;

final class AdjacentMetadataDetectorIsExplicit {
    static boolean isExplicit(Object raw) {
        return raw instanceof Map<?, ?> map && "explicit".equals(map.get("mode"));
    }
}

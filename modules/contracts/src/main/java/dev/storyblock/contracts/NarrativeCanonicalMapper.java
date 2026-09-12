package dev.storyblock.contracts;

import dev.storyblock.domain.RevisionManifest;
import java.util.Map;

public final class NarrativeCanonicalMapper {
    NarrativeCanonicalMapper() {
    }

    public static CanonicalRevision toCanonical(RevisionManifest manifest) {
        return NarrativeCanonicalMapperToCanonicalFactory.toCanonical(manifest);
    }

    public static RevisionManifest fromCanonical(CanonicalRevision canonical) {
        return NarrativeCanonicalMapperFromCanonicalFactory.fromCanonical(canonical);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> requireMap(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

}

package dev.storyblock.contracts;

import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.RevisionEntry;

final class CanonicalNovelPackageRevisionMap {
    static Map<String, Object> revisionMap(RevisionEntry value) {
        return Map.of(
                "sequence", value.sequence(),
                "document", value.revision().envelope()
        );
    }
}

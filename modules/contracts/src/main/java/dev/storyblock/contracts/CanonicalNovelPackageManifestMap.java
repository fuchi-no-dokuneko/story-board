package dev.storyblock.contracts;

import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.Manifest;

final class CanonicalNovelPackageManifestMap {
    static Map<String, Object> manifestMap(Manifest value) {
        return Map.of(
                "novel_id", value.novelId().value(),
                "schema_version", value.schemaVersion(),
                "head_revision_id", value.headRevisionId().value(),
                "head_sequence", value.headSequence(),
                "head_hash", value.headHash(),
                "revision_count", value.revisionCount(),
                "operation_count", value.operationCount(),
                "artifact_count", value.artifactCount()
        );
    }
}

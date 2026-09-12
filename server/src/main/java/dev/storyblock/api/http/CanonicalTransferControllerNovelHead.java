package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.storage.CanonicalImportResult;
import java.util.Map;

final class CanonicalTransferControllerNovelHead {
    static Map<String, Object> novelHead(CanonicalImportResult result) {
        return Map.of(
                "novel_id", result.novelId().value(),
                "head_revision_id", result.head().revisionId().value(),
                "head_sequence", result.head().sequence(),
                "head_hash", result.head().contentHash(),
                "schema_version", CanonicalRevision.SCHEMA_VERSION
        );
    }
}

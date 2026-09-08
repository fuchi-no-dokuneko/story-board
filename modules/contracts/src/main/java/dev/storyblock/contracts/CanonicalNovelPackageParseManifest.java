package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.Manifest;
import static dev.storyblock.contracts.CanonicalNovelPackage.MANIFEST_FIELDS;

final class CanonicalNovelPackageParseManifest {
    static Manifest parseManifest(Map<String, Object> value) {
        CanonicalNovelPackageRequireKeys.requireKeys(value, MANIFEST_FIELDS, "manifest");
        return new Manifest(
                new Ids.NovelId(CanonicalNovelPackageString.string(value, "novel_id", "manifest")),
                CanonicalNovelPackageString.string(value, "schema_version", "manifest"),
                new Ids.RevisionId(CanonicalNovelPackageString.string(value, "head_revision_id", "manifest")),
                CanonicalNovelPackageExactLong.exactLong(value.get("head_sequence"), "manifest.head_sequence"),
                CanonicalNovelPackageString.string(value, "head_hash", "manifest"),
                CanonicalNovelPackageExactInt.exactInt(value.get("revision_count"), "manifest.revision_count"),
                CanonicalNovelPackageExactInt.exactInt(value.get("operation_count"), "manifest.operation_count"),
                CanonicalNovelPackageExactInt.exactInt(value.get("artifact_count"), "manifest.artifact_count")
        );
    }
}

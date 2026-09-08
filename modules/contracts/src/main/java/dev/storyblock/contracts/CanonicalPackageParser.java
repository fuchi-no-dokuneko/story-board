package dev.storyblock.contracts;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import static dev.storyblock.contracts.CanonicalNovelPackage.*;
import static dev.storyblock.contracts.CanonicalPackageFields.ROOT_FIELDS;

final class CanonicalPackageParser {
    static CanonicalNovelPackage parse(byte[] json) {
        Objects.requireNonNull(json, "json");
        if (json.length == 0 || json.length > MAX_PACKAGE_BYTES) {
            throw new CanonicalPackageException("Canonical package size is invalid");
        }
        final Map<String, Object> root;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = CanonicalJson.mapper().readValue(json, Map.class);
            if (parsed == null) {
                throw new CanonicalPackageException("Canonical package must be an object");
            }
            root = parsed;
        } catch (RuntimeException failure) {
            throw new CanonicalPackageException("Canonical package JSON is malformed", failure);
        }
        CanonicalNovelPackageRequireKeys.requireKeys(root, ROOT_FIELDS, "package");

        Manifest manifest = CanonicalNovelPackageParseManifest.parseManifest(CanonicalPackageObject.object(root.get("manifest"), "manifest"));
        List<RevisionEntry> revisions = CanonicalNovelPackageArray.array(root.get("revisions"), "revisions").stream()
                .map(value -> CanonicalNovelPackageParseRevision.parseRevision(CanonicalPackageObject.object(value, "revision")))
                .toList();
        List<OperationEntry> operations = CanonicalNovelPackageArray.array(root.get("operations"), "operations").stream()
                .map(value -> CanonicalNovelPackageParseOperation.parseOperation(CanonicalPackageObject.object(value, "operation entry")))
                .toList();
        List<ArtifactEntry> artifacts = CanonicalNovelPackageArray.array(root.get("artifacts"), "artifacts").stream()
                .map(value -> CanonicalNovelPackageParseArtifact.parseArtifact(CanonicalPackageObject.object(value, "artifact")))
                .toList();
        return new CanonicalNovelPackage(
                CanonicalNovelPackageString.string(root, "package_version", "package"),
                manifest,
                revisions,
                operations,
                artifacts
        );
    }


}

package dev.storyblock.contracts;

import java.util.LinkedHashMap;
import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.*;

final class CanonicalPackageEncoding {
    static Map<String, Object> encode(CanonicalNovelPackage document) {
        var manifest = document.manifest();
        var revisions = document.revisions();
        var operations = document.operations();
        var artifacts = document.artifacts();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("package_version", PACKAGE_VERSION);
        root.put("manifest", CanonicalNovelPackageManifestMap.manifestMap(manifest));
        root.put("revisions", revisions.stream().map(CanonicalNovelPackageRevisionMap::revisionMap).toList());
        root.put("operations", operations.stream().map(CanonicalNovelPackageOperationMap::operationMap).toList());
        root.put("artifacts", artifacts.stream().map(CanonicalNovelPackageArtifactMap::artifactMap).toList());
        return Map.copyOf(root);
    }


}

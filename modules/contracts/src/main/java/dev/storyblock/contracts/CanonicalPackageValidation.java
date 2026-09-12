package dev.storyblock.contracts;

import static dev.storyblock.contracts.CanonicalNovelPackage.*;

final class CanonicalPackageValidation {
    static void validate(CanonicalNovelPackage document) {
        var revisions = document.revisions();
        var operations = document.operations();
        var artifacts = document.artifacts();
        if (revisions.isEmpty()) {
            throw new CanonicalPackageException("Canonical package has no revisions");
        }
        if (revisions.size() > MAX_REVISIONS) {
            throw new CanonicalPackageException("Canonical package has too many revisions");
        }
        if (operations.size() != revisions.size() - 1) {
            throw new CanonicalPackageException(
                    "Every revision after genesis must have exactly one operation"
            );
        }
        if (artifacts.size() > MAX_ARTIFACTS) {
            throw new CanonicalPackageException("Canonical package has too many artifacts");
        }

        var lineage = CanonicalPackageLineage.validate(document);
        CanonicalPackageOperations.validate(document, lineage.manifests());
        CanonicalPackageManifest.validate(document, lineage.manifests());
        CanonicalPackageArtifacts.validate(document, lineage.revisionIds());
    }

}

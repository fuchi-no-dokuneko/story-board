package dev.storyblock.contracts;

import dev.storyblock.domain.RevisionManifest;
import java.util.List;
import static dev.storyblock.contracts.CanonicalNovelPackage.*;

final class CanonicalPackageManifest {
    static void validate(CanonicalNovelPackage document, List<RevisionManifest> manifests) {
        var revisions = document.revisions();
        var operations = document.operations();
        var artifacts = document.artifacts();
        var manifest = document.manifest();
        RevisionEntry head = revisions.getLast();
        RevisionManifest headManifest = manifests.getLast();
        if (!CanonicalRevision.SCHEMA_VERSION.equals(manifest.schemaVersion())
                || !manifest.headRevisionId().equals(headManifest.id())
                || manifest.headSequence() != head.sequence()
                || !manifest.headHash().equals(head.revision().contentHash())
                || manifest.revisionCount() != revisions.size()
                || manifest.operationCount() != operations.size()
                || manifest.artifactCount() != artifacts.size()) {
            throw new CanonicalPackageException("Package manifest does not match its contents");
        }

    }

}

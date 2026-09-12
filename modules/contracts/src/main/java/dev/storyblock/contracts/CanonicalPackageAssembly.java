package dev.storyblock.contracts;

import dev.storyblock.domain.RevisionManifest;
import java.util.List;
import java.util.Objects;
import static dev.storyblock.contracts.CanonicalNovelPackage.*;

final class CanonicalPackageAssembly {
    static CanonicalNovelPackage assemble(
            List<RevisionEntry> revisions,
            List<OperationEntry> operations,
            List<ArtifactEntry> artifacts
    ) {
        Objects.requireNonNull(revisions, "revisions");
        if (revisions.isEmpty()) {
            throw new CanonicalPackageException("Canonical package has no revisions");
        }
        RevisionEntry head = revisions.getLast();
        RevisionManifest headManifest = NarrativeCanonicalMapper.fromCanonical(head.revision());
        Manifest manifest = new Manifest(
                headManifest.novel().id(),
                CanonicalRevision.SCHEMA_VERSION,
                headManifest.id(),
                head.sequence(),
                head.revision().contentHash(),
                revisions.size(),
                operations.size(),
                artifacts.size()
        );
        return new CanonicalNovelPackage(
                PACKAGE_VERSION, manifest, revisions, operations, artifacts
        );
    }

    static CanonicalNovelPackage genesis(CanonicalRevision revision) {
        Objects.requireNonNull(revision, "revision");
        RevisionManifest manifest = NarrativeCanonicalMapper.fromCanonical(revision);
        if (manifest.parentId() != null) {
            throw new CanonicalPackageException(
                    "A standalone canonical revision must be a genesis revision"
            );
        }
        return assemble(List.of(new RevisionEntry(0, revision)), List.of(), List.of());
    }


}

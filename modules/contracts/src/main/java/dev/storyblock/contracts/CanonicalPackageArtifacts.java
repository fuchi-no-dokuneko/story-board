package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.Set;
import static dev.storyblock.contracts.CanonicalNovelPackage.*;

final class CanonicalPackageArtifacts {
    static void validate(CanonicalNovelPackage document, Set<Ids.RevisionId> revisionIds) {
        var artifacts = document.artifacts();
        Set<Ids.ArtifactId> artifactIds = new HashSet<>();
        for (ArtifactEntry artifact : artifacts) {
            if (!artifactIds.add(artifact.artifactId())) {
                throw new CanonicalPackageException("Duplicate artifact ID");
            }
            if (!revisionIds.contains(artifact.revisionId())) {
                throw new CanonicalPackageException("Artifact references an unknown revision");
            }
        }
    }

}

package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import java.util.Objects;

public interface CanonicalManifestTypes {
    public record Manifest(
            Ids.NovelId novelId,
            String schemaVersion,
            Ids.RevisionId headRevisionId,
            long headSequence,
            String headHash,
            int revisionCount,
            int operationCount,
            int artifactCount
    ) {
        public Manifest {
            Objects.requireNonNull(novelId, "novelId");
            Objects.requireNonNull(schemaVersion, "schemaVersion");
            Objects.requireNonNull(headRevisionId, "headRevisionId");
            if (headSequence < 0 || revisionCount < 1
                    || operationCount < 0 || artifactCount < 0) {
                throw new CanonicalPackageException("Package manifest counts are invalid");
            }
            CanonicalNovelPackageRequireHash.requireHash(headHash, "Manifest head hash");
        }
    }
}

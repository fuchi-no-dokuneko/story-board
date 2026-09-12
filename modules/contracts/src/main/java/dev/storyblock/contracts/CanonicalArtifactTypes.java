package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;
import static dev.storyblock.contracts.CanonicalNovelPackage.MAX_ARTIFACT_BYTES;
import static dev.storyblock.contracts.CanonicalPackageFields.MEDIA_TYPE;

public interface CanonicalArtifactTypes {
    public record ArtifactEntry(
            Ids.ArtifactId artifactId,
            Ids.RevisionId revisionId,
            String kind,
            String mediaType,
            String codec,
            String contentHash,
            byte[] content,
            Instant createdAt
    ) {
        public ArtifactEntry {
            Objects.requireNonNull(artifactId, "artifactId");
            Objects.requireNonNull(revisionId, "revisionId");
            kind = CanonicalNovelPackageRequireToken.requireToken(kind, "Artifact kind");
            codec = CanonicalNovelPackageRequireToken.requireToken(codec, "Artifact codec");
            if (mediaType == null || !MEDIA_TYPE.matcher(mediaType).matches()) {
                throw new CanonicalPackageException("Artifact media type is invalid");
            }
            CanonicalNovelPackageRequireHash.requireHash(contentHash, "Artifact content hash");
            content = Objects.requireNonNull(content, "content").clone();
            if (content.length > MAX_ARTIFACT_BYTES) {
                throw new CanonicalPackageException("Artifact exceeds the package size limit");
            }
            if (!CanonicalJson.hashBytes(content).equals(contentHash)) {
                throw new CanonicalPackageException("Artifact hash does not match content");
            }
            Objects.requireNonNull(createdAt, "createdAt");
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}

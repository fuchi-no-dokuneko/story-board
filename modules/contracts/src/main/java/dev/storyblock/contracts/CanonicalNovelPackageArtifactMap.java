package dev.storyblock.contracts;

import java.util.Base64;
import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.ArtifactEntry;

final class CanonicalNovelPackageArtifactMap {
    static Map<String, Object> artifactMap(ArtifactEntry value) {
        return Map.of(
                "artifact_id", value.artifactId().value(),
                "revision_id", value.revisionId().value(),
                "kind", value.kind(),
                "media_type", value.mediaType(),
                "codec", value.codec(),
                "content_hash", value.contentHash(),
                "size_bytes", value.content().length,
                "created_at", value.createdAt().toString(),
                "content_base64", Base64.getEncoder().encodeToString(value.content())
        );
    }
}

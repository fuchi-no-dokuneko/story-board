package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import java.util.Base64;
import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.ARTIFACT_FIELDS;
import static dev.storyblock.contracts.CanonicalNovelPackage.ArtifactEntry;

final class CanonicalNovelPackageParseArtifact {
    static ArtifactEntry parseArtifact(Map<String, Object> value) {
        CanonicalNovelPackageRequireKeys.requireKeys(value, ARTIFACT_FIELDS, "artifact");
        String encoded = CanonicalNovelPackageString.string(value, "content_base64", "artifact");
        final byte[] content;
        try {
            content = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException failure) {
            throw new CanonicalPackageException("Artifact content_base64 is invalid", failure);
        }
        if (!Base64.getEncoder().encodeToString(content).equals(encoded)) {
            throw new CanonicalPackageException("Artifact base64 must use canonical encoding");
        }
        if (CanonicalNovelPackageExactInt.exactInt(value.get("size_bytes"), "artifact.size_bytes") != content.length) {
            throw new CanonicalPackageException("Artifact size_bytes does not match content");
        }
        return new ArtifactEntry(
                new Ids.ArtifactId(CanonicalNovelPackageString.string(value, "artifact_id", "artifact")),
                new Ids.RevisionId(CanonicalNovelPackageString.string(value, "revision_id", "artifact")),
                CanonicalNovelPackageString.string(value, "kind", "artifact"),
                CanonicalNovelPackageString.string(value, "media_type", "artifact"),
                CanonicalNovelPackageString.string(value, "codec", "artifact"),
                CanonicalNovelPackageString.string(value, "content_hash", "artifact"),
                content,
                CanonicalNovelPackageInstant.instant(value, "created_at", "artifact")
        );
    }
}

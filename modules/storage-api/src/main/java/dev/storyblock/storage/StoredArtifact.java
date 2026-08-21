package dev.storyblock.storage;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record StoredArtifact(
        Ids.ArtifactId artifactId,
        Ids.NovelId novelId,
        Ids.RevisionId revisionId,
        String kind,
        String mediaType,
        String codec,
        String contentHash,
        byte[] content,
        Instant createdAt,
        boolean portable
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern TOKEN = Pattern.compile("[a-z][a-z0-9.-]{1,63}");
    private static final Pattern MEDIA_TYPE = Pattern.compile(
            "[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+"
    );

    public StoredArtifact {
        Objects.requireNonNull(artifactId, "artifactId");
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        requireToken(kind, "Artifact kind");
        requireToken(codec, "Artifact codec");
        if (mediaType == null || !MEDIA_TYPE.matcher(mediaType).matches()) {
            throw new CanonicalPackageException("Artifact media type is invalid");
        }
        if (contentHash == null || !SHA_256.matcher(contentHash).matches()) {
            throw new CanonicalPackageException(
                    "Artifact content hash must be lowercase SHA-256"
            );
        }
        content = Objects.requireNonNull(content, "content").clone();
        int limit = portable
                ? CanonicalNovelPackage.MAX_ARTIFACT_BYTES
                : CanonicalNovelPackage.MAX_PACKAGE_BYTES;
        if (content.length > limit) {
            throw new CanonicalPackageException("Artifact exceeds its storage size limit");
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

    public CanonicalNovelPackage.ArtifactEntry toPackageEntry() {
        if (!portable) {
            throw new IllegalStateException("Generated export artifacts are not portable");
        }
        return new CanonicalNovelPackage.ArtifactEntry(
                artifactId,
                revisionId,
                kind,
                mediaType,
                codec,
                contentHash,
                content,
                createdAt
        );
    }

    private static void requireToken(String value, String field) {
        if (value == null || !TOKEN.matcher(value).matches()) {
            throw new CanonicalPackageException(field + " is not a canonical token");
        }
    }
}

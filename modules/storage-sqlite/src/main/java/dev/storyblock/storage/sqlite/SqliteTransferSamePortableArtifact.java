package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StoredArtifact;

final class SqliteTransferSamePortableArtifact {
    static boolean samePortableArtifact(
            StoredArtifact stored,
            StoredArtifact attempted
    ) {
        return stored.artifactId().equals(attempted.artifactId())
                && stored.novelId().equals(attempted.novelId())
                && stored.revisionId().equals(attempted.revisionId())
                && stored.kind().equals(attempted.kind())
                && stored.mediaType().equals(attempted.mediaType())
                && stored.codec().equals(attempted.codec())
                && stored.contentHash().equals(attempted.contentHash())
                && java.util.Arrays.equals(stored.content(), attempted.content())
                && stored.portable()
                && attempted.portable();
    }
}

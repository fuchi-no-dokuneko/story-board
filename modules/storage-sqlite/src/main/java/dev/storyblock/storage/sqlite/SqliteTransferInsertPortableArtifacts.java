package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.storage.StoredArtifact;
import java.sql.Connection;
import java.sql.SQLException;

final class SqliteTransferInsertPortableArtifacts {
    static void insertPortableArtifacts(
            Connection connection,
            CanonicalNovelPackage document
    ) throws SQLException {
        for (CanonicalNovelPackage.ArtifactEntry entry : document.artifacts()) {
            SqliteTransferInsertArtifact.insertArtifact(connection, new StoredArtifact(
                    entry.artifactId(),
                    document.manifest().novelId(),
                    entry.revisionId(),
                    entry.kind(),
                    entry.mediaType(),
                    entry.codec(),
                    entry.contentHash(),
                    entry.content(),
                    entry.createdAt(),
                    true
            ));
        }
    }
}

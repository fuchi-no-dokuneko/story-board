package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.RevisionManifest;
import java.sql.PreparedStatement;
import java.util.Objects;

final class SqliteRevisionStoreCreateNovelAction {
    static void createNovel(SqliteRevisionStore self, RevisionManifest initialRevision, String contentHash)  {
        Objects.requireNonNull(initialRevision, "initialRevision");
        if (initialRevision.parentId() != null) {
            throw new IllegalArgumentException("Initial revision cannot have a parent");
        }
        CanonicalRevision canonical = NarrativeCanonicalMapper.toCanonical(initialRevision);
        if (!canonical.contentHash().equals(contentHash)) {
            throw new IllegalArgumentException("Initial revision content hash does not match canon");
        }
        byte[] envelope = canonical.envelopeBytes();
        self.write(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO novels(
                        novel_id, head_revision_id, head_sequence, head_hash, schema_version
                    ) VALUES (?, ?, 0, ?, ?)
                    """)) {
                statement.setString(1, initialRevision.novel().id().value());
                statement.setString(2, initialRevision.id().value());
                statement.setString(3, contentHash);
                statement.setString(4, CanonicalRevision.SCHEMA_VERSION);
                statement.executeUpdate();
            }
            SqliteRevisionStoreInsertRevision.insertRevision(connection, initialRevision, 0, contentHash, envelope, null);
            SqliteRevisionStoreRebuildProjection.rebuildProjection(connection, initialRevision);
            SqliteRevisionStoreInsertCheckpoint.insertCheckpoint(connection, initialRevision, 0, contentHash, envelope);
            return null;
        });
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.RevisionManifest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteRevisionStoreInsertCheckpoint {
    static void insertCheckpoint(
            Connection connection,
            RevisionManifest revision,
            long sequence,
            String contentHash,
            byte[] canonicalJson
    ) throws SQLException {
        byte[] compressed = GzipCheckpointCodec.compress(canonicalJson);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO checkpoints(
                    novel_id, revision_id, sequence, content_hash, codec,
                    uncompressed_bytes, compressed_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, revision.novel().id().value());
            statement.setString(2, revision.id().value());
            statement.setLong(3, sequence);
            statement.setString(4, contentHash);
            statement.setString(5, GzipCheckpointCodec.NAME);
            statement.setInt(6, canonicalJson.length);
            statement.setBytes(7, compressed);
            statement.setString(8, revision.createdAt().toString());
            statement.executeUpdate();
        }
    }
}

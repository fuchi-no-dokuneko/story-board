package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteTransferRebuildProjection {
    static void rebuildProjection(Connection connection, RevisionManifest revision)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO head_block_projection(
                    novel_id, chapter_id, scene_id, block_id, block_version_id,
                    order_key, text_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (NarrativeChapter chapter : revision.novel().chapters()) {
                for (NarrativeScene scene : chapter.scenes()) {
                    for (NarrativeBlock block : scene.blocks()) {
                        statement.setString(1, revision.novel().id().value());
                        statement.setString(2, chapter.id().value());
                        statement.setString(3, scene.id().value());
                        statement.setString(4, block.id().value());
                        statement.setString(5, block.versionId().value());
                        statement.setString(6, block.orderKey().value());
                        statement.setString(7, CanonicalJson.hash(block.text()));
                        statement.addBatch();
                    }
                }
            }
            statement.executeBatch();
        }
    }
}

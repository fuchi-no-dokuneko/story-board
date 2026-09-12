package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteRevisionStoreRebuildProjection {
    static void rebuildProjection(
            Connection connection,
            RevisionManifest revision
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM head_block_projection WHERE novel_id = ?"
        )) {
            delete.setString(1, revision.novel().id().value());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO head_block_projection(
                    novel_id, chapter_id, scene_id, block_id, block_version_id,
                    order_key, text_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (NarrativeChapter chapter : revision.novel().chapters()) {
                for (NarrativeScene scene : chapter.scenes()) {
                    for (NarrativeBlock block : scene.blocks()) {
                        insert.setString(1, revision.novel().id().value());
                        insert.setString(2, chapter.id().value());
                        insert.setString(3, scene.id().value());
                        insert.setString(4, block.id().value());
                        insert.setString(5, block.versionId().value());
                        insert.setString(6, block.orderKey().value());
                        insert.setString(7, CanonicalJson.hash(block.text()));
                        insert.addBatch();
                    }
                }
            }
            insert.executeBatch();
        }
    }
}

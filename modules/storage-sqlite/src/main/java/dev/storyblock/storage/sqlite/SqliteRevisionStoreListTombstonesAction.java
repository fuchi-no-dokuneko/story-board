package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.BlockTombstone;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

final class SqliteRevisionStoreListTombstonesAction {
    static List<BlockTombstone> listTombstones(SqliteRevisionStore self, Ids.NovelId novelId)  {
        return self.read(connection -> {
            List<BlockTombstone> tombstones = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT operation_id, deleted_in_revision_id, source_scene_id, block_json
                    FROM block_tombstones
                    WHERE novel_id = ?
                    ORDER BY rowid
                    """)) {
                statement.setString(1, novelId.value());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        tombstones.add(new BlockTombstone(
                                novelId,
                                new Ids.OperationId(result.getString("operation_id")),
                                new Ids.RevisionId(result.getString("deleted_in_revision_id")),
                                new Ids.SceneId(result.getString("source_scene_id")),
                                SqliteRevisionStoreParseBlock.parseBlock(result.getString("block_json"))
                        ));
                    }
                }
            }
            return List.copyOf(tombstones);
        });
    }
}

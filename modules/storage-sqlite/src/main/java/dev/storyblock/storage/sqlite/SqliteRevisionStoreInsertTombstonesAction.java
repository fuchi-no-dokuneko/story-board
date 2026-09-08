package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.CommitRequest;
import dev.storyblock.storage.StoredRevision;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import static dev.storyblock.storage.sqlite.SqliteRevisionStore.LocatedBlock;

final class SqliteRevisionStoreInsertTombstonesAction {
    static void insertTombstones(SqliteRevisionStore self, Connection connection, CommitRequest request) throws SQLException {
        StoredRevision base = SqliteRevisionStoreGetRevisionFactory.getRevision(connection,
                request.operation().context().novelId(), request.expectedHead().revisionId());
        Map<Ids.BlockId, LocatedBlock> previous = SqliteRevisionStoreLocateBlocks.locateBlocks(base.manifest());
        Map<Ids.BlockId, LocatedBlock> current = SqliteRevisionStoreLocateBlocks.locateBlocks(request.candidate());
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO block_tombstones(
                    novel_id, operation_id, deleted_in_revision_id, source_scene_id,
                    block_id, block_version_id, block_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (Map.Entry<Ids.BlockId, LocatedBlock> entry : previous.entrySet()) {
                if (current.containsKey(entry.getKey())) {
                    continue;
                }
                LocatedBlock deleted = entry.getValue();
                statement.setString(1, request.operation().context().novelId().value());
                statement.setString(2, request.operation().context().operationId().value());
                statement.setString(3, request.candidate().id().value());
                statement.setString(4, deleted.sceneId().value());
                statement.setString(5, deleted.block().id().value());
                statement.setString(6, deleted.block().versionId().value());
                statement.setString(7, CanonicalJson.string(SqliteRevisionStoreBlockToMap.blockToMap(deleted.block())));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import static dev.storyblock.storage.sqlite.SqliteTransferData.*;

final class SqliteTransferRebuildTombstones {
    static void rebuildTombstones(
            Connection connection,
            CanonicalNovelPackage document
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO block_tombstones(
                    novel_id, operation_id, deleted_in_revision_id, source_scene_id,
                    block_id, block_version_id, block_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (int index = 1; index < document.revisions().size(); index++) {
                RevisionManifest previous = NarrativeCanonicalMapper.fromCanonical(
                        document.revisions().get(index - 1).revision()
                );
                RevisionManifest current = NarrativeCanonicalMapper.fromCanonical(
                        document.revisions().get(index).revision()
                );
                Ids.OperationId operationId = document.operations().get(index - 1)
                        .operation().context().operationId();
                Map<Ids.BlockId, LocatedBlock> previousBlocks = SqliteTransferLocateBlocks.locateBlocks(previous);
                Map<Ids.BlockId, LocatedBlock> currentBlocks = SqliteTransferLocateBlocks.locateBlocks(current);
                for (Map.Entry<Ids.BlockId, LocatedBlock> block : previousBlocks.entrySet()) {
                    if (currentBlocks.containsKey(block.getKey())) {
                        continue;
                    }
                    LocatedBlock deleted = block.getValue();
                    statement.setString(1, document.manifest().novelId().value());
                    statement.setString(2, operationId.value());
                    statement.setString(3, current.id().value());
                    statement.setString(4, deleted.sceneId().value());
                    statement.setString(5, deleted.block().id().value());
                    statement.setString(6, deleted.block().versionId().value());
                    statement.setString(7, CanonicalJson.string(SqliteTransferBlockToMap.blockToMap(deleted.block())));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }
}

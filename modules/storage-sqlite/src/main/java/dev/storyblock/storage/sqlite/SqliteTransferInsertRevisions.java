package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

final class SqliteTransferInsertRevisions {
    static void insertRevisions(Connection connection, CanonicalNovelPackage document)
            throws SQLException {
        Map<Long, Ids.OperationId> operationIds = new LinkedHashMap<>();
        document.operations().forEach(entry -> operationIds.put(
                entry.sequence(), entry.operation().context().operationId()
        ));
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO revisions(
                    revision_id, novel_id, parent_revision_id, sequence, content_hash,
                    canonical_json, created_at, operation_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (CanonicalNovelPackage.RevisionEntry entry : document.revisions()) {
                RevisionManifest revision = NarrativeCanonicalMapper.fromCanonical(entry.revision());
                SqliteRevisionIdentities.claim(connection, revision, entry.revision().contentHash());
                statement.setString(1, revision.id().value());
                statement.setString(2, revision.novel().id().value());
                statement.setString(3, revision.parentId() == null
                        ? null : revision.parentId().value());
                statement.setLong(4, entry.sequence());
                statement.setString(5, entry.revision().contentHash());
                statement.setBytes(6, entry.revision().envelopeBytes());
                statement.setString(7, revision.createdAt().toString());
                Ids.OperationId operationId = operationIds.get(entry.sequence());
                statement.setString(8, operationId == null ? null : operationId.value());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}

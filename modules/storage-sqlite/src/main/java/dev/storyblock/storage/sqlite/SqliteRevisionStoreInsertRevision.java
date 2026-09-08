package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteRevisionStoreInsertRevision {
    static void insertRevision(
            Connection connection,
            RevisionManifest revision,
            long sequence,
            String contentHash,
            byte[] canonicalJson,
            Ids.OperationId operationId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO revisions(
                    revision_id, novel_id, parent_revision_id, sequence, content_hash,
                    canonical_json, created_at, operation_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, revision.id().value());
            statement.setString(2, revision.novel().id().value());
            statement.setString(3, revision.parentId() == null ? null : revision.parentId().value());
            statement.setLong(4, sequence);
            statement.setString(5, contentHash);
            statement.setBytes(6, canonicalJson);
            statement.setString(7, revision.createdAt().toString());
            statement.setString(8, operationId == null ? null : operationId.value());
            statement.executeUpdate();
        }
    }
}

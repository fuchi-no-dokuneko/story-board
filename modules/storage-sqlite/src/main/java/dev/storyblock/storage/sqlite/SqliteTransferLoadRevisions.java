package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.StorageException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class SqliteTransferLoadRevisions {
    static List<CanonicalNovelPackage.RevisionEntry> loadRevisions(
            Connection connection,
            Ids.NovelId novelId
    ) throws SQLException {
        List<CanonicalNovelPackage.RevisionEntry> revisions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT revision_id, parent_revision_id, sequence, content_hash,
                       canonical_json, created_at
                FROM revisions
                WHERE novel_id = ?
                ORDER BY sequence
                """)) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    CanonicalRevision revision = CanonicalRevision.parseEnvelope(
                            result.getBytes("canonical_json")
                    );
                    if (!revision.contentHash().equals(result.getString("content_hash"))) {
                        throw new StorageException("Stored revision hash does not match package data");
                    }
                    RevisionManifest manifest = NarrativeCanonicalMapper.fromCanonical(revision);
                    if (!manifest.novel().id().equals(novelId)
                            || !manifest.id().value().equals(result.getString("revision_id"))
                            || !Objects.equals(
                                    manifest.parentId() == null ? null : manifest.parentId().value(),
                                    result.getString("parent_revision_id")
                            )
                            || !manifest.createdAt().toString().equals(result.getString("created_at"))) {
                        throw new StorageException("Stored revision identity does not match package data");
                    }
                    revisions.add(new CanonicalNovelPackage.RevisionEntry(
                            result.getLong("sequence"), revision
                    ));
                }
            }
        }
        return List.copyOf(revisions);
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StorageException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

final class SqliteTransferLoadPackage {
    static CanonicalNovelPackage loadPackage(Connection connection, Ids.NovelId novelId)
            throws SQLException {
        RevisionRef relationalHead = SqliteTransferRequireHead.requireHead(connection, novelId);
        List<CanonicalNovelPackage.RevisionEntry> revisions = SqliteTransferLoadRevisions.loadRevisions(
                connection, novelId
        );
        List<CanonicalNovelPackage.OperationEntry> operations = SqliteTransferLoadOperations.loadOperations(
                connection, novelId, relationalHead.sequence()
        );
        List<CanonicalNovelPackage.ArtifactEntry> artifacts = SqliteTransferLoadPortableArtifacts.loadPortableArtifacts(
                connection, novelId
        );
        CanonicalNovelPackage document = CanonicalNovelPackage.assemble(
                revisions, operations, artifacts
        );
        if (!document.manifest().headRevisionId().equals(relationalHead.revisionId())
                || document.manifest().headSequence() != relationalHead.sequence()
                || !document.manifest().headHash().equals(relationalHead.contentHash())) {
            throw new StorageException("Canonical package does not match the relational head");
        }
        return document;
    }
}

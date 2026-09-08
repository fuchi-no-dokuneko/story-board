package dev.storyblock.storage.sqlite;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.storage.*;
import java.sql.*;
import static dev.storyblock.storage.sqlite.SqliteTransferData.*;
final class SqliteTransferReplayImport {
    static CanonicalImportResult replay(Connection connection, CanonicalImportRequest request,
            CanonicalNovelPackage document, ImportReceipt receipt) throws SQLException {
        if (!receipt.requestHash().equals(request.requestHash())) {
            throw new IdempotencyConflictException(
                    request.idempotencyKey(), receipt.requestHash(), request.requestHash()
            );
        }
        if (!receipt.novelId().equals(document.manifest().novelId())) {
            throw new StorageException("Import receipt does not match the canonical novel");
        }
        RevisionRef originalHead = SqliteTransferRequireRevision.requireRevision(
                connection, receipt.novelId(), document.manifest().headRevisionId()
        );
        RevisionRef expectedHead = new RevisionRef(
                document.manifest().headRevisionId(),
                document.manifest().headSequence(),
                document.manifest().headHash()
        );
        if (!originalHead.equals(expectedHead)) {
            throw new StorageException("Import receipt does not match its original head");
        }
        return new CanonicalImportResult(
                receipt.novelId(), originalHead, true
        );
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteTransferData.*;

final class SqliteTransferImportPackage {
    static CanonicalImportResult importPackage(
            Connection connection,
            CanonicalImportRequest request,
            ImportFaultInjector faultInjector
    ) throws SQLException {
        CanonicalNovelPackage document = request.document();
        Optional<ImportReceipt> prior = SqliteTransferFindImportReceipt.findImportReceipt(
                connection, request.idempotencyKey()
        );
        faultInjector.after(ImportStage.AFTER_RECEIPT_CHECK);
        if (prior.isPresent()) {
            return SqliteTransferReplayImport.replay(connection, request, document, prior.get());
        }

        Ids.NovelId novelId = document.manifest().novelId();
        if (SqliteTransferNovelExists.novelExists(connection, novelId)) {
            throw new NovelConflictException(novelId);
        }

        SqliteTransferInsertNovel.insertNovel(connection, document);
        faultInjector.after(ImportStage.AFTER_NOVEL);
        SqliteTransferInsertRevisions.insertRevisions(connection, document);
        faultInjector.after(ImportStage.AFTER_REVISIONS);
        SqliteTransferInsertOperations.insertOperations(connection, document);
        faultInjector.after(ImportStage.AFTER_OPERATIONS);
        SqliteTransferInsertPortableArtifacts.insertPortableArtifacts(connection, document);
        faultInjector.after(ImportStage.AFTER_ARTIFACTS);
        SqliteTransferRebuildTombstones.rebuildTombstones(connection, document);
        faultInjector.after(ImportStage.AFTER_TOMBSTONES);
        CanonicalNovelPackage.RevisionEntry head = document.revisions().getLast();
        RevisionManifest headManifest = NarrativeCanonicalMapper.fromCanonical(head.revision());
        SqliteTransferInsertCheckpoint.insertCheckpoint(connection, headManifest, head.sequence(), head.revision());
        faultInjector.after(ImportStage.AFTER_CHECKPOINT);
        SqliteTransferRebuildProjection.rebuildProjection(connection, headManifest);
        faultInjector.after(ImportStage.AFTER_PROJECTION);
        SqliteTransferInsertImportReceipt.insertImportReceipt(connection, request, novelId);
        faultInjector.after(ImportStage.AFTER_RECEIPT);

        return new CanonicalImportResult(
                novelId,
                new RevisionRef(
                        document.manifest().headRevisionId(),
                        document.manifest().headSequence(),
                        document.manifest().headHash()
                ),
                false
        );
    }
}

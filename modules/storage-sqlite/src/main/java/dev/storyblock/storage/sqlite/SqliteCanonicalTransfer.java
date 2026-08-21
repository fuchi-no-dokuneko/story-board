package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.CanonicalImportRequest;
import dev.storyblock.storage.CanonicalImportResult;
import dev.storyblock.storage.ExportJobRequest;
import dev.storyblock.storage.ExportJobResult;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.MissingArtifactException;
import dev.storyblock.storage.MissingExportJobException;
import dev.storyblock.storage.NovelConflictException;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StorageException;
import dev.storyblock.storage.StoredArtifact;
import dev.storyblock.storage.StoredExportJob;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class SqliteCanonicalTransfer {
    private SqliteCanonicalTransfer() {
    }

    static CanonicalNovelPackage loadPackage(Connection connection, Ids.NovelId novelId)
            throws SQLException {
        RevisionRef relationalHead = requireHead(connection, novelId);
        List<CanonicalNovelPackage.RevisionEntry> revisions = loadRevisions(
                connection, novelId
        );
        List<CanonicalNovelPackage.OperationEntry> operations = loadOperations(
                connection, novelId, relationalHead.sequence()
        );
        List<CanonicalNovelPackage.ArtifactEntry> artifacts = loadPortableArtifacts(
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

    static CanonicalImportResult importPackage(
            Connection connection,
            CanonicalImportRequest request,
            ImportFaultInjector faultInjector
    ) throws SQLException {
        CanonicalNovelPackage document = request.document();
        Optional<ImportReceipt> prior = findImportReceipt(
                connection, request.idempotencyKey()
        );
        faultInjector.after(ImportStage.AFTER_RECEIPT_CHECK);
        if (prior.isPresent()) {
            ImportReceipt receipt = prior.get();
            if (!receipt.requestHash().equals(request.requestHash())) {
                throw new IdempotencyConflictException(
                        request.idempotencyKey(), receipt.requestHash(), request.requestHash()
                );
            }
            if (!receipt.novelId().equals(document.manifest().novelId())) {
                throw new StorageException("Import receipt does not match the canonical novel");
            }
            RevisionRef originalHead = requireRevision(
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

        Ids.NovelId novelId = document.manifest().novelId();
        if (novelExists(connection, novelId)) {
            throw new NovelConflictException(novelId);
        }

        insertNovel(connection, document);
        faultInjector.after(ImportStage.AFTER_NOVEL);
        insertRevisions(connection, document);
        faultInjector.after(ImportStage.AFTER_REVISIONS);
        insertOperations(connection, document);
        faultInjector.after(ImportStage.AFTER_OPERATIONS);
        insertPortableArtifacts(connection, document);
        faultInjector.after(ImportStage.AFTER_ARTIFACTS);
        rebuildTombstones(connection, document);
        faultInjector.after(ImportStage.AFTER_TOMBSTONES);
        CanonicalNovelPackage.RevisionEntry head = document.revisions().getLast();
        RevisionManifest headManifest = NarrativeCanonicalMapper.fromCanonical(head.revision());
        insertCheckpoint(connection, headManifest, head.sequence(), head.revision());
        faultInjector.after(ImportStage.AFTER_CHECKPOINT);
        rebuildProjection(connection, headManifest);
        faultInjector.after(ImportStage.AFTER_PROJECTION);
        insertImportReceipt(connection, request, novelId);
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

    static ExportJobResult createCompletedExport(
            Connection connection,
            ExportJobRequest request
    ) throws SQLException {
        Optional<StoredExportWithHash> prior = findExportByIdempotencyKey(
                connection, request.novelId(), request.idempotencyKey()
        );
        if (prior.isPresent()) {
            StoredExportWithHash stored = prior.get();
            if (!stored.requestHash().equals(request.requestHash())) {
                throw new IdempotencyConflictException(
                        request.idempotencyKey(), stored.requestHash(), request.requestHash()
                );
            }
            return new ExportJobResult(stored.job(), true);
        }

        RevisionRef actualHead = requireHead(connection, request.novelId());
        if (!actualHead.equals(request.expectedHead())) {
            throw new StaleHeadException(request.expectedHead(), actualHead);
        }
        insertArtifact(connection, request.artifact());
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO export_jobs(
                    job_id, novel_id, revision_id, revision_sequence, revision_hash,
                    format, idempotency_key, request_hash, result_artifact_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, request.jobId().value());
            statement.setString(2, request.novelId().value());
            statement.setString(3, request.expectedHead().revisionId().value());
            statement.setLong(4, request.expectedHead().sequence());
            statement.setString(5, request.expectedHead().contentHash());
            statement.setString(6, request.format().canonicalName());
            statement.setString(7, request.idempotencyKey());
            statement.setString(8, request.requestHash());
            statement.setString(9, request.artifact().artifactId().value());
            statement.setString(10, request.createdAt().toString());
            statement.executeUpdate();
        }
        return new ExportJobResult(toStoredJob(request), false);
    }

    static StoredExportJob getExportJob(Connection connection, Ids.JobId jobId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT job_id, novel_id, revision_id, revision_sequence, revision_hash,
                       format, result_artifact_id, created_at
                FROM export_jobs
                WHERE job_id = ?
                """)) {
            statement.setString(1, jobId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingExportJobException(jobId);
                }
                return readExportJob(result);
            }
        }
    }

    static StoredArtifact getArtifact(Connection connection, Ids.ArtifactId artifactId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT artifact_id, novel_id, revision_id, kind, media_type, codec,
                       content_hash, content, created_at, portable
                FROM artifacts
                WHERE artifact_id = ?
                """)) {
            statement.setString(1, artifactId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingArtifactException(artifactId);
                }
                return readArtifact(result);
            }
        }
    }

    private static List<CanonicalNovelPackage.RevisionEntry> loadRevisions(
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

    private static List<CanonicalNovelPackage.OperationEntry> loadOperations(
            Connection connection,
            Ids.NovelId novelId,
            long throughSequence
    ) throws SQLException {
        List<CanonicalNovelPackage.OperationEntry> operations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT operation_id, novel_id, base_revision_id, operation_type,
                       idempotency_key, sequence, operation_hash, payload_json,
                       result_revision_id, result_hash, committed_at
                FROM operations
                WHERE novel_id = ? AND sequence <= ?
                ORDER BY sequence
                """)) {
            statement.setString(1, novelId.value());
            statement.setLong(2, throughSequence);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    EditOperation operation = readOperation(result);
                    operations.add(new CanonicalNovelPackage.OperationEntry(
                            result.getLong("sequence"),
                            result.getString("operation_hash"),
                            operation,
                            new Ids.RevisionId(result.getString("result_revision_id")),
                            result.getString("result_hash"),
                            Instant.parse(result.getString("committed_at"))
                    ));
                }
            }
        }
        return List.copyOf(operations);
    }

    private static List<CanonicalNovelPackage.ArtifactEntry> loadPortableArtifacts(
            Connection connection,
            Ids.NovelId novelId
    ) throws SQLException {
        List<CanonicalNovelPackage.ArtifactEntry> artifacts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT artifact_id, novel_id, revision_id, kind, media_type, codec,
                       content_hash, content, created_at, portable
                FROM artifacts
                WHERE novel_id = ? AND portable = 1
                ORDER BY artifact_id
                """)) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    artifacts.add(readArtifact(result).toPackageEntry());
                }
            }
        }
        return List.copyOf(artifacts);
    }

    private static void insertNovel(Connection connection, CanonicalNovelPackage document)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO novels(
                    novel_id, head_revision_id, head_sequence, head_hash, schema_version
                ) VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, document.manifest().novelId().value());
            statement.setString(2, document.manifest().headRevisionId().value());
            statement.setLong(3, document.manifest().headSequence());
            statement.setString(4, document.manifest().headHash());
            statement.setString(5, document.manifest().schemaVersion());
            statement.executeUpdate();
        }
    }

    private static void insertRevisions(Connection connection, CanonicalNovelPackage document)
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

    private static void insertOperations(Connection connection, CanonicalNovelPackage document)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO operations(
                    operation_id, novel_id, sequence, base_revision_id, operation_type,
                    operation_hash, idempotency_key, payload_json, result_revision_id,
                    result_hash, committed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (CanonicalNovelPackage.OperationEntry entry : document.operations()) {
                EditOperation operation = entry.operation();
                statement.setString(1, operation.context().operationId().value());
                statement.setString(2, operation.context().novelId().value());
                statement.setLong(3, entry.sequence());
                statement.setString(4, operation.context().baseRevisionId().value());
                statement.setString(5, operation.type().canonicalName());
                statement.setString(6, entry.operationHash());
                statement.setString(7, operation.context().idempotencyKey());
                statement.setString(8, CanonicalJson.string(
                        EditOperationCanonicalMapper.toCanonical(operation)
                ));
                statement.setString(9, entry.resultRevisionId().value());
                statement.setString(10, entry.resultHash());
                statement.setString(11, entry.committedAt().toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertPortableArtifacts(
            Connection connection,
            CanonicalNovelPackage document
    ) throws SQLException {
        for (CanonicalNovelPackage.ArtifactEntry entry : document.artifacts()) {
            insertArtifact(connection, new StoredArtifact(
                    entry.artifactId(),
                    document.manifest().novelId(),
                    entry.revisionId(),
                    entry.kind(),
                    entry.mediaType(),
                    entry.codec(),
                    entry.contentHash(),
                    entry.content(),
                    entry.createdAt(),
                    true
            ));
        }
    }

    private static void insertArtifact(Connection connection, StoredArtifact artifact)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO artifacts(
                    artifact_id, novel_id, revision_id, kind, media_type, codec,
                    content_hash, size_bytes, content, created_at, portable
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, artifact.artifactId().value());
            statement.setString(2, artifact.novelId().value());
            statement.setString(3, artifact.revisionId().value());
            statement.setString(4, artifact.kind());
            statement.setString(5, artifact.mediaType());
            statement.setString(6, artifact.codec());
            statement.setString(7, artifact.contentHash());
            statement.setInt(8, artifact.content().length);
            statement.setBytes(9, artifact.content());
            statement.setString(10, artifact.createdAt().toString());
            statement.setInt(11, artifact.portable() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    private static void rebuildTombstones(
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
                Map<Ids.BlockId, LocatedBlock> previousBlocks = locateBlocks(previous);
                Map<Ids.BlockId, LocatedBlock> currentBlocks = locateBlocks(current);
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
                    statement.setString(7, CanonicalJson.string(blockToMap(deleted.block())));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private static void insertCheckpoint(
            Connection connection,
            RevisionManifest revision,
            long sequence,
            CanonicalRevision canonical
    ) throws SQLException {
        byte[] envelope = canonical.envelopeBytes();
        byte[] compressed = GzipCheckpointCodec.compress(envelope);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO checkpoints(
                    novel_id, revision_id, sequence, content_hash, codec,
                    uncompressed_bytes, compressed_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, revision.novel().id().value());
            statement.setString(2, revision.id().value());
            statement.setLong(3, sequence);
            statement.setString(4, canonical.contentHash());
            statement.setString(5, GzipCheckpointCodec.NAME);
            statement.setInt(6, envelope.length);
            statement.setBytes(7, compressed);
            statement.setString(8, revision.createdAt().toString());
            statement.executeUpdate();
        }
    }

    private static void rebuildProjection(Connection connection, RevisionManifest revision)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO head_block_projection(
                    novel_id, chapter_id, scene_id, block_id, block_version_id,
                    order_key, text_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (NarrativeChapter chapter : revision.novel().chapters()) {
                for (NarrativeScene scene : chapter.scenes()) {
                    for (NarrativeBlock block : scene.blocks()) {
                        statement.setString(1, revision.novel().id().value());
                        statement.setString(2, chapter.id().value());
                        statement.setString(3, scene.id().value());
                        statement.setString(4, block.id().value());
                        statement.setString(5, block.versionId().value());
                        statement.setString(6, block.orderKey().value());
                        statement.setString(7, CanonicalJson.hash(block.text()));
                        statement.addBatch();
                    }
                }
            }
            statement.executeBatch();
        }
    }

    private static void insertImportReceipt(
            Connection connection,
            CanonicalImportRequest request,
            Ids.NovelId novelId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO import_receipts(
                    idempotency_key, request_hash, novel_id, imported_at
                ) VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, request.idempotencyKey());
            statement.setString(2, request.requestHash());
            statement.setString(3, novelId.value());
            statement.setString(4, request.importedAt().toString());
            statement.executeUpdate();
        }
    }

    private static Optional<ImportReceipt> findImportReceipt(
            Connection connection,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT request_hash, novel_id
                FROM import_receipts
                WHERE idempotency_key = ?
                """)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new ImportReceipt(
                                result.getString("request_hash"),
                                new Ids.NovelId(result.getString("novel_id"))
                        ))
                        : Optional.empty();
            }
        }
    }

    private static Optional<StoredExportWithHash> findExportByIdempotencyKey(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT job_id, novel_id, revision_id, revision_sequence, revision_hash,
                       format, result_artifact_id, created_at, request_hash
                FROM export_jobs
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new StoredExportWithHash(
                                readExportJob(result), result.getString("request_hash")
                        ))
                        : Optional.empty();
            }
        }
    }

    private static StoredExportJob readExportJob(ResultSet result) throws SQLException {
        return new StoredExportJob(
                new Ids.JobId(result.getString("job_id")),
                new Ids.NovelId(result.getString("novel_id")),
                new RevisionRef(
                        new Ids.RevisionId(result.getString("revision_id")),
                        result.getLong("revision_sequence"),
                        result.getString("revision_hash")
                ),
                CanonicalExportFormat.fromCanonicalName(result.getString("format")),
                new Ids.ArtifactId(result.getString("result_artifact_id")),
                Instant.parse(result.getString("created_at"))
        );
    }

    private static StoredArtifact readArtifact(ResultSet result) throws SQLException {
        return new StoredArtifact(
                new Ids.ArtifactId(result.getString("artifact_id")),
                new Ids.NovelId(result.getString("novel_id")),
                new Ids.RevisionId(result.getString("revision_id")),
                result.getString("kind"),
                result.getString("media_type"),
                result.getString("codec"),
                result.getString("content_hash"),
                result.getBytes("content"),
                Instant.parse(result.getString("created_at")),
                result.getInt("portable") == 1
        );
    }

    private static StoredExportJob toStoredJob(ExportJobRequest request) {
        return new StoredExportJob(
                request.jobId(),
                request.novelId(),
                request.expectedHead(),
                request.format(),
                request.artifact().artifactId(),
                request.createdAt()
        );
    }

    private static EditOperation readOperation(ResultSet result) throws SQLException {
        EditOperation operation = EditOperationCanonicalMapper.fromCanonical(
                result.getString("payload_json").getBytes(StandardCharsets.UTF_8)
        );
        String storedHash = result.getString("operation_hash");
        if (!EditOperationCanonicalMapper.hash(operation).equals(storedHash)
                || !operation.context().operationId().value().equals(
                        result.getString("operation_id")
                )
                || !operation.context().novelId().value().equals(result.getString("novel_id"))
                || !operation.context().baseRevisionId().value().equals(
                        result.getString("base_revision_id")
                )
                || !operation.type().canonicalName().equals(result.getString("operation_type"))
                || !operation.context().idempotencyKey().equals(
                        result.getString("idempotency_key")
                )) {
            throw new StorageException("Stored operation identity does not match package data");
        }
        return operation;
    }

    private static RevisionRef requireHead(Connection connection, Ids.NovelId novelId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT head_revision_id, head_sequence, head_hash
                FROM novels
                WHERE novel_id = ?
                """)) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new dev.storyblock.storage.MissingNovelException(novelId);
                }
                return new RevisionRef(
                        new Ids.RevisionId(result.getString("head_revision_id")),
                        result.getLong("head_sequence"),
                        result.getString("head_hash")
                );
            }
        }
    }

    private static RevisionRef requireRevision(
            Connection connection,
            Ids.NovelId novelId,
            Ids.RevisionId revisionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sequence, content_hash
                FROM revisions
                WHERE novel_id = ? AND revision_id = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, revisionId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new dev.storyblock.storage.MissingRevisionException(
                            novelId, revisionId
                    );
                }
                return new RevisionRef(
                        revisionId,
                        result.getLong("sequence"),
                        result.getString("content_hash")
                );
            }
        }
    }

    private static boolean novelExists(Connection connection, Ids.NovelId novelId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM novels WHERE novel_id = ?"
        )) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static Map<Ids.BlockId, LocatedBlock> locateBlocks(RevisionManifest revision) {
        Map<Ids.BlockId, LocatedBlock> blocks = new LinkedHashMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (NarrativeBlock block : scene.blocks()) {
                    blocks.put(block.id(), new LocatedBlock(scene.id(), block));
                }
            }
        }
        return blocks;
    }

    private static Map<String, Object> blockToMap(NarrativeBlock block) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", block.id().value());
        value.put("block_version_id", block.versionId().value());
        value.put("order_key", block.orderKey().value());
        value.put("text", block.text());
        value.put("meta", block.metadata().fields());
        if (!block.extensions().isEmpty()) {
            value.put("extensions", block.extensions());
        }
        return value;
    }

    private record LocatedBlock(Ids.SceneId sceneId, NarrativeBlock block) {
    }

    private record ImportReceipt(String requestHash, Ids.NovelId novelId) {
    }

    private record StoredExportWithHash(StoredExportJob job, String requestHash) {
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.security.AccessKeyInsertResult;
import dev.storyblock.security.AccessKeyStore;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.security.StoredAccessKey;
import dev.storyblock.monitor.MonitorSaveResult;
import dev.storyblock.monitor.MonitorStore;
import dev.storyblock.monitor.StoredMonitorRun;
import dev.storyblock.rewrite.policy.ReserveRewriteCandidateCommand;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCandidateReservationSaveResult;
import dev.storyblock.rewrite.policy.RewriteReservationStore;
import dev.storyblock.style.CreateStyleProfileCommand;
import dev.storyblock.style.CreateStyleProfileVersionCommand;
import dev.storyblock.style.StyleAnalysisClaimCommand;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisCompletionResult;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobSaveResult;
import dev.storyblock.style.StyleAnalysisLease;
import dev.storyblock.style.StyleAnalysisResult;
import dev.storyblock.style.StyleAnalysisStore;
import dev.storyblock.style.StyleAnalysisWindowSlice;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileSaveResult;
import dev.storyblock.style.StyleProfileStore;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import dev.storyblock.style.StyleProfileVersionView;
import dev.storyblock.style.TransitionStyleProfileVersionCommand;
import dev.storyblock.storage.BlockTombstone;
import dev.storyblock.storage.CanonicalImportRequest;
import dev.storyblock.storage.CanonicalImportResult;
import dev.storyblock.storage.CommitRequest;
import dev.storyblock.storage.CommitResult;
import dev.storyblock.storage.ExportJobRequest;
import dev.storyblock.storage.ExportJobResult;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.MissingNovelException;
import dev.storyblock.storage.MissingRevisionException;
import dev.storyblock.storage.PortableArtifactPutRequest;
import dev.storyblock.storage.PortableArtifactPutResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StorageException;
import dev.storyblock.storage.StoredCheckpoint;
import dev.storyblock.storage.StoredArtifact;
import dev.storyblock.storage.StoredExportJob;
import dev.storyblock.storage.StoredOperation;
import dev.storyblock.storage.StoredRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

public final class SqliteRevisionStore implements
        RevisionStore, AccessKeyStore, MonitorStore, StyleProfileStore,
        StyleAnalysisStore, RewriteReservationStore, AutoCloseable {
    private final SqliteDatabase database;
    private final CheckpointPolicy checkpointPolicy;
    private final CommitFaultInjector faultInjector;
    private final ImportFaultInjector importFaultInjector;

    private SqliteRevisionStore(
            SqliteDatabase database,
            CheckpointPolicy checkpointPolicy,
            CommitFaultInjector faultInjector,
            ImportFaultInjector importFaultInjector
    ) {
        this.database = Objects.requireNonNull(database, "database");
        this.checkpointPolicy = Objects.requireNonNull(checkpointPolicy, "checkpointPolicy");
        this.faultInjector = Objects.requireNonNull(faultInjector, "faultInjector");
        this.importFaultInjector = Objects.requireNonNull(
                importFaultInjector, "importFaultInjector"
        );
        write(connection -> {
            return null;
        });
    }

    public static SqliteRevisionStore open(Path databasePath) throws IOException {
        return open(databasePath, CheckpointPolicy.DEFAULT);
    }

    public static SqliteRevisionStore open(
            Path databasePath,
            CheckpointPolicy checkpointPolicy
    ) throws IOException {
        return new SqliteRevisionStore(
                SqliteDatabase.open(databasePath),
                checkpointPolicy,
                CommitFaultInjector.NONE,
                ImportFaultInjector.NONE
        );
    }

    static SqliteRevisionStore open(
            Path databasePath,
            CheckpointPolicy checkpointPolicy,
            CommitFaultInjector faultInjector
    ) throws IOException {
        return new SqliteRevisionStore(
                SqliteDatabase.open(databasePath),
                checkpointPolicy,
                faultInjector,
                ImportFaultInjector.NONE
        );
    }

    static SqliteRevisionStore open(
            Path databasePath,
            CheckpointPolicy checkpointPolicy,
            CommitFaultInjector faultInjector,
            ImportFaultInjector importFaultInjector
    ) throws IOException {
        return new SqliteRevisionStore(
                SqliteDatabase.open(databasePath),
                checkpointPolicy,
                faultInjector,
                importFaultInjector
        );
    }

    @Override
    public void createNovel(RevisionManifest initialRevision, String contentHash) {
        Objects.requireNonNull(initialRevision, "initialRevision");
        if (initialRevision.parentId() != null) {
            throw new IllegalArgumentException("Initial revision cannot have a parent");
        }
        CanonicalRevision canonical = NarrativeCanonicalMapper.toCanonical(initialRevision);
        if (!canonical.contentHash().equals(contentHash)) {
            throw new IllegalArgumentException("Initial revision content hash does not match canon");
        }
        byte[] envelope = canonical.envelopeBytes();
        write(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO novels(
                        novel_id, head_revision_id, head_sequence, head_hash, schema_version
                    ) VALUES (?, ?, 0, ?, ?)
                    """)) {
                statement.setString(1, initialRevision.novel().id().value());
                statement.setString(2, initialRevision.id().value());
                statement.setString(3, contentHash);
                statement.setString(4, CanonicalRevision.SCHEMA_VERSION);
                statement.executeUpdate();
            }
            insertRevision(connection, initialRevision, 0, contentHash, envelope, null);
            rebuildProjection(connection, initialRevision);
            insertCheckpoint(connection, initialRevision, 0, contentHash, envelope);
            return null;
        });
    }

    @Override
    public List<Ids.NovelId> listNovels() {
        return read(connection -> {
            List<Ids.NovelId> novels = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT novel_id FROM novels ORDER BY novel_id"
            ); ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    novels.add(new Ids.NovelId(result.getString(1)));
                }
            }
            return List.copyOf(novels);
        });
    }

    @Override
    public RevisionRef getHead(Ids.NovelId novelId) {
        return read(connection -> requireHead(connection, novelId));
    }

    @Override
    public StoredRevision getRevision(Ids.NovelId novelId, Ids.RevisionId revisionId) {
        return read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT parent_revision_id, sequence, content_hash,
                           canonical_json, created_at
                    FROM revisions
                    WHERE novel_id = ? AND revision_id = ?
                    """)) {
                statement.setString(1, novelId.value());
                statement.setString(2, revisionId.value());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new MissingRevisionException(novelId, revisionId);
                    }
                    return readRevision(result, novelId, revisionId);
                }
            }
        });
    }

    @Override
    public StoredRevision getRevisionAtSequence(Ids.NovelId novelId, long sequence) {
        return read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT revision_id, parent_revision_id, sequence, content_hash,
                           canonical_json, created_at
                    FROM revisions
                    WHERE novel_id = ? AND sequence = ?
                    """)) {
                statement.setString(1, novelId.value());
                statement.setLong(2, sequence);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new StorageException(
                                "Novel " + novelId.value() + " has no revision sequence " + sequence
                        );
                    }
                    return readRevision(
                            result, novelId, new Ids.RevisionId(result.getString("revision_id"))
                    );
                }
            }
        });
    }

    @Override
    public Optional<StoredOperation> findByIdempotencyKey(Ids.NovelId novelId, String key) {
        return read(connection -> findByIdempotencyKey(connection, novelId, key));
    }

    @Override
    public Optional<StoredCheckpoint> loadCheckpoint(
            Ids.NovelId novelId,
            long atOrBeforeSequence
    ) {
        return read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT revision_id, sequence, content_hash, codec,
                           uncompressed_bytes, compressed_json
                    FROM checkpoints
                    WHERE novel_id = ? AND sequence <= ?
                    ORDER BY sequence DESC
                    LIMIT 1
                    """)) {
                statement.setString(1, novelId.value());
                statement.setLong(2, atOrBeforeSequence);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new StoredCheckpoint(
                            novelId,
                            new Ids.RevisionId(result.getString("revision_id")),
                            result.getLong("sequence"),
                            result.getString("content_hash"),
                            result.getString("codec"),
                            result.getInt("uncompressed_bytes"),
                            result.getBytes("compressed_json")
                    ));
                }
            }
        });
    }

    @Override
    public byte[] decompressCheckpoint(StoredCheckpoint checkpoint) {
        if (!GzipCheckpointCodec.NAME.equals(checkpoint.codec())) {
            throw new StorageException("Unsupported checkpoint codec " + checkpoint.codec());
        }
        return GzipCheckpointCodec.decompress(
                checkpoint.compressedCanonicalJson(), checkpoint.uncompressedBytes()
        );
    }

    @Override
    public List<StoredOperation> listOperations(
            Ids.NovelId novelId,
            long afterSequence,
            long throughSequence
    ) {
        if (afterSequence < 0 || throughSequence < afterSequence) {
            throw new IllegalArgumentException("Invalid operation sequence range");
        }
        return read(connection -> {
            List<StoredOperation> operations = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT operation_id, novel_id, base_revision_id, operation_type,
                           idempotency_key, sequence, operation_hash, payload_json,
                           result_revision_id, result_hash, committed_at
                    FROM operations
                    WHERE novel_id = ? AND sequence > ? AND sequence <= ?
                    ORDER BY sequence
                    """)) {
                statement.setString(1, novelId.value());
                statement.setLong(2, afterSequence);
                statement.setLong(3, throughSequence);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        operations.add(readOperation(result));
                    }
                }
            }
            return List.copyOf(operations);
        });
    }

    @Override
    public List<BlockTombstone> listTombstones(Ids.NovelId novelId) {
        return read(connection -> {
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
                                parseBlock(result.getString("block_json"))
                        ));
                    }
                }
            }
            return List.copyOf(tombstones);
        });
    }

    @Override
    public dev.storyblock.contracts.CanonicalNovelPackage loadCanonicalPackage(
            Ids.NovelId novelId
    ) {
        return read(connection -> SqliteCanonicalTransfer.loadPackage(connection, novelId));
    }

    @Override
    public CanonicalImportResult importCanonicalPackage(CanonicalImportRequest request) {
        Objects.requireNonNull(request, "request");
        return write(connection -> SqliteCanonicalTransfer.importPackage(
                connection, request, importFaultInjector
        ));
    }

    @Override
    public ExportJobResult createCompletedExport(ExportJobRequest request) {
        Objects.requireNonNull(request, "request");
        return write(connection -> SqliteCanonicalTransfer.createCompletedExport(
                connection, request
        ));
    }

    @Override
    public StoredExportJob getExportJob(Ids.JobId jobId) {
        return read(connection -> SqliteCanonicalTransfer.getExportJob(connection, jobId));
    }

    @Override
    public StoredArtifact getArtifact(Ids.ArtifactId artifactId) {
        return read(connection -> SqliteCanonicalTransfer.getArtifact(connection, artifactId));
    }

    @Override
    public PortableArtifactPutResult putPortableArtifact(PortableArtifactPutRequest request) {
        Objects.requireNonNull(request, "request");
        return write(connection -> SqliteCanonicalTransfer.putPortableArtifact(
                connection, request
        ));
    }

    @Override
    public AccessKeyInsertResult issueAccessKey(
            StoredAccessKey key,
            String idempotencyKey,
            String requestHash,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(auditContext, "auditContext");
        return write(connection -> SqliteSecurityStore.issueAccessKey(
                connection, key, idempotencyKey, requestHash, auditContext
        ));
    }

    @Override
    public Optional<StoredAccessKey> findAccessKey(Ids.AccessKeyId keyId) {
        Objects.requireNonNull(keyId, "keyId");
        return read(connection -> SqliteSecurityStore.findAccessKey(connection, keyId));
    }

    @Override
    public boolean revokeAccessKey(
            Ids.AccessKeyId keyId,
            Ids.NovelId expectedNovelId,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(expectedNovelId, "expectedNovelId");
        Objects.requireNonNull(auditContext, "auditContext");
        return write(connection -> SqliteSecurityStore.revokeAccessKey(
                connection, keyId, expectedNovelId, auditContext
        ));
    }

    @Override
    public boolean touchAccessKeyLastUsed(
            Ids.AccessKeyId keyId,
            Instant usedAt,
            Instant staleBefore
    ) {
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(usedAt, "usedAt");
        Objects.requireNonNull(staleBefore, "staleBefore");
        return write(connection -> SqliteSecurityStore.touchAccessKeyLastUsed(
                connection, keyId, usedAt, staleBefore
        ));
    }

    @Override
    public void appendAuditEvent(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        write(connection -> {
            SqliteSecurityStore.insertAuditEvent(connection, event);
            return null;
        });
    }

    @Override
    public List<AuditEvent> listAuditEvents(Ids.NovelId novelId) {
        Objects.requireNonNull(novelId, "novelId");
        return read(connection -> SqliteSecurityStore.listAuditEvents(
                connection, novelId
        ));
    }

    @Override
    public CommitResult commitCas(CommitRequest request) {
        Objects.requireNonNull(request, "request");
        return commitCas(
                request,
                AuditContext.system(
                        "req_internal_" + request.operation().context().operationId().value(),
                        request.candidate().createdAt()
                )
        );
    }

    @Override
    public CommitResult commitCas(CommitRequest request, AuditContext auditContext) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(auditContext, "auditContext");
        verifyRequestHashes(request);
        return write(connection -> commit(connection, request, auditContext));
    }

    @Override
    public void recordCommitReplayAudit(
            StoredOperation operation,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(auditContext, "auditContext");
        write(connection -> {
            SqliteSecurityStore.insertAuditEvent(connection, commitAuditEvent(
                    operation.operation().context().novelId(),
                    operation.operation().context().operationId(),
                    operation.resultRevisionId(),
                    operation.operationHash(),
                    operation.resultHash(),
                    AuditResult.IDEMPOTENT,
                    auditContext
            ));
            return null;
        });
    }

    @Override
    public MonitorSaveResult saveMonitorRun(StoredMonitorRun run) {
        Objects.requireNonNull(run, "run");
        return write(connection -> SqliteMonitorStore.save(connection, run));
    }

    @Override
    public StoredMonitorRun getMonitorRun(
            Ids.NovelId novelId,
            Ids.MonitorRunId runId
    ) {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(runId, "runId");
        return read(connection -> SqliteMonitorStore.get(connection, novelId, runId));
    }

    @Override
    public StyleProfileSaveResult createStyleProfile(CreateStyleProfileCommand command) {
        Objects.requireNonNull(command, "command");
        return write(connection -> SqliteStyleProfileStore.createProfile(
                connection, command
        ));
    }

    @Override
    public StyleProfile getStyleProfile(Ids.StyleProfileId profileId) {
        Objects.requireNonNull(profileId, "profileId");
        return read(connection -> SqliteStyleProfileStore.getProfile(
                connection, profileId
        ));
    }

    @Override
    public StyleProfileVersionSaveResult createStyleProfileVersion(
            CreateStyleProfileVersionCommand command
    ) {
        Objects.requireNonNull(command, "command");
        return write(connection -> SqliteStyleProfileStore.createVersion(
                connection, command
        ));
    }

    @Override
    public StyleProfileVersionView getStyleProfileVersion(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(versionId, "versionId");
        return read(connection -> SqliteStyleProfileStore.getVersion(
                connection, profileId, versionId
        ));
    }

    @Override
    public StyleProfileVersionSaveResult transitionStyleProfileVersion(
            TransitionStyleProfileVersionCommand command
    ) {
        Objects.requireNonNull(command, "command");
        return write(connection -> SqliteStyleProfileStore.transition(
                connection, command
        ));
    }

    @Override
    public StyleAnalysisJobSaveResult createStyleAnalysisJob(StyleAnalysisJob job) {
        Objects.requireNonNull(job, "job");
        return write(connection -> SqliteStyleAnalysisStore.createJob(connection, job));
    }

    @Override
    public StyleAnalysisJob getStyleAnalysisJob(Ids.JobId jobId) {
        Objects.requireNonNull(jobId, "jobId");
        return read(connection -> SqliteStyleAnalysisStore.getJob(connection, jobId));
    }

    @Override
    public StyleAnalysisJob getStyleAnalysis(Ids.StyleAnalysisId analysisId) {
        Objects.requireNonNull(analysisId, "analysisId");
        return read(connection -> SqliteStyleAnalysisStore.getAnalysis(
                connection, analysisId
        ));
    }

    @Override
    public Optional<StyleAnalysisLease> claimStyleAnalysis(
            StyleAnalysisClaimCommand command
    ) {
        Objects.requireNonNull(command, "command");
        return write(connection -> SqliteStyleAnalysisStore.claim(connection, command));
    }

    @Override
    public StyleAnalysisCompletionResult completeStyleAnalysis(
            StyleAnalysisCompletionCommand command
    ) {
        Objects.requireNonNull(command, "command");
        return write(connection -> SqliteStyleAnalysisStore.complete(
                connection, command
        ));
    }

    @Override
    public StyleAnalysisJob failStyleAnalysis(
            Ids.JobId jobId,
            String leaseOwner,
            int attempt,
            String expectedStatusHash,
            String failureCode,
            Instant failedAt
    ) {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(failedAt, "failedAt");
        return write(connection -> SqliteStyleAnalysisStore.fail(
                connection,
                jobId,
                leaseOwner,
                attempt,
                expectedStatusHash,
                failureCode,
                failedAt
        ));
    }

    @Override
    public Optional<StyleAnalysisResult> findStyleAnalysisResult(
            Ids.StyleAnalysisId analysisId
    ) {
        Objects.requireNonNull(analysisId, "analysisId");
        return read(connection -> SqliteStyleAnalysisStore.findResult(
                connection, analysisId
        ));
    }

    @Override
    public StyleAnalysisWindowSlice listStyleAnalysisWindows(
            Ids.StyleAnalysisId analysisId,
            int afterOrdinal,
            int limit
    ) {
        Objects.requireNonNull(analysisId, "analysisId");
        return read(connection -> SqliteStyleAnalysisStore.listWindows(
                connection, analysisId, afterOrdinal, limit
        ));
    }

    @Override
    public Optional<Instant> findStyleArtifactExpiry(Ids.ArtifactId artifactId) {
        Objects.requireNonNull(artifactId, "artifactId");
        return read(connection -> SqliteStyleAnalysisStore.findArtifactExpiry(
                connection, artifactId
        ));
    }

    @Override
    public RewriteCandidateReservationSaveResult reserveRewriteCandidate(
            ReserveRewriteCandidateCommand command
    ) {
        Objects.requireNonNull(command, "command");
        return write(connection -> SqliteRewriteReservationStore.reserve(
                connection, command
        ));
    }

    @Override
    public RewriteCandidateReservation getRewriteCandidateReservation(
            Ids.ProposalId proposalId
    ) {
        Objects.requireNonNull(proposalId, "proposalId");
        return read(connection -> SqliteRewriteReservationStore.get(
                connection, proposalId
        ));
    }

    @Override
    public long revisionCount(Ids.NovelId novelId) {
        return count(novelId, "revisions");
    }

    @Override
    public long operationCount(Ids.NovelId novelId) {
        return count(novelId, "operations");
    }

    public SqliteOperationalSnapshot operationalSnapshot(Instant now) {
        Objects.requireNonNull(now, "now");
        SqliteMetrics.Snapshot metrics = database.metrics();
        long walBytes;
        try {
            walBytes = database.walBytes();
        } catch (IOException exception) {
            throw new StorageException("Could not inspect the SQLite WAL", exception);
        }
        long capturedWalBytes = walBytes;
        return read(connection -> {
            long[] values = new long[5];
            String migrationVersion;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT
                      (SELECT COUNT(*) FROM analysis_jobs WHERE status = 'queued'),
                      CAST(COALESCE((julianday(?) - julianday(
                        (SELECT MIN(created_at) FROM analysis_jobs WHERE status = 'queued')
                      )) * 86400, 0) AS INTEGER),
                      CAST(COALESCE((SELECT AVG(
                        (julianday(r.completed_at) - julianday(j.created_at)) * 86400000
                      ) FROM analysis_runs r JOIN analysis_jobs j ON j.job_id = r.job_id), 0)
                        AS INTEGER),
                      (SELECT COUNT(*) FROM rewrite_proposals WHERE state = 'stale'),
                      (SELECT COALESCE(SUM(size_bytes), 0) FROM artifacts)
                    """)) {
                statement.setString(1, now.toString());
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    for (int index = 0; index < values.length; index++) {
                        values[index] = result.getLong(index + 1);
                    }
                }
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT version FROM flyway_schema_history
                         WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1
                         """)) {
                migrationVersion = result.next() ? result.getString(1) : "none";
            }
            Map<String, Long> findings = new LinkedHashMap<>();
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT code, COUNT(*) AS total FROM issues GROUP BY code ORDER BY code
                         """)) {
                while (result.next()) {
                    findings.put(result.getString("code"), result.getLong("total"));
                }
            }
            return new SqliteOperationalSnapshot(
                    metrics.writerWaitMillis(),
                    metrics.maxTransactionMillis(),
                    metrics.sqliteBusyTotal(),
                    capturedWalBytes,
                    metrics.lastCheckpointMillis(),
                    values[0],
                    values[1],
                    values[2],
                    0L,
                    values[3],
                    values[4],
                    migrationVersion,
                    findings
            );
        });
    }

    public void verifyReadableAndWritable() {
        write(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT 1")) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new SQLException("SQLite health query returned an invalid result");
                }
            }
            return null;
        });
    }

    public SqliteWalCheckpoint checkpointPassive() {
        try {
            return database.checkpointPassive();
        } catch (SQLException exception) {
            throw new StorageException("Could not inspect the SQLite checkpoint", exception);
        }
    }

    @Override
    public void close() {
        database.close();
    }

    private CommitResult commit(
            Connection connection,
            CommitRequest request,
            AuditContext auditContext
    ) throws SQLException {
        Ids.NovelId novelId = request.operation().context().novelId();
        Optional<StoredOperation> prior = findByIdempotencyKey(
                connection, novelId, request.operation().context().idempotencyKey()
        );
        faultInjector.after(CommitStage.AFTER_IDEMPOTENCY_CHECK);
        if (prior.isPresent()) {
            StoredOperation stored = prior.get();
            if (!stored.operationHash().equals(request.operationHash())) {
                throw new IdempotencyConflictException(
                        request.operation().context().idempotencyKey(),
                        stored.operationHash(),
                        request.operationHash()
                );
            }
            CommitResult result = new CommitResult(
                    new RevisionRef(
                            stored.resultRevisionId(), stored.sequence(), stored.resultHash()
                    ),
                    stored.operation().context().operationId(),
                    true
            );
            SqliteSecurityStore.insertAuditEvent(connection, commitAuditEvent(
                    novelId,
                    stored.operation().context().operationId(),
                    stored.resultRevisionId(),
                    stored.operationHash(),
                    stored.resultHash(),
                    AuditResult.IDEMPOTENT,
                    auditContext
            ));
            faultInjector.after(CommitStage.AFTER_AUDIT);
            return result;
        }

        RevisionRef actualHead = requireHead(connection, novelId);
        if (!actualHead.equals(request.expectedHead())) {
            throw new StaleHeadException(request.expectedHead(), actualHead);
        }
        long sequence = actualHead.sequence() + 1;
        byte[] operationBytes = CanonicalJson.bytes(
                EditOperationCanonicalMapper.toCanonical(request.operation())
        );
        byte[] candidateBytes = NarrativeCanonicalMapper.toCanonical(request.candidate()).envelopeBytes();

        insertOperation(connection, request, sequence, operationBytes);
        faultInjector.after(CommitStage.AFTER_OPERATION_APPEND);
        insertRevision(
                connection,
                request.candidate(),
                sequence,
                request.candidateHash(),
                candidateBytes,
                request.operation().context().operationId()
        );
        faultInjector.after(CommitStage.AFTER_REVISION_APPEND);
        insertTombstones(connection, request);
        faultInjector.after(CommitStage.AFTER_TOMBSTONES);
        rebuildProjection(connection, request.candidate());
        faultInjector.after(CommitStage.AFTER_PROJECTION);
        if (shouldCheckpoint(connection, novelId, sequence)) {
            insertCheckpoint(
                    connection,
                    request.candidate(),
                    sequence,
                    request.candidateHash(),
                    candidateBytes
            );
        }
        faultInjector.after(CommitStage.AFTER_CHECKPOINT);
        faultInjector.after(CommitStage.BEFORE_HEAD_CAS);

        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE novels
                SET head_revision_id = ?, head_sequence = ?, head_hash = ?
                WHERE novel_id = ? AND head_revision_id = ? AND head_hash = ?
                """)) {
            statement.setString(1, request.candidate().id().value());
            statement.setLong(2, sequence);
            statement.setString(3, request.candidateHash());
            statement.setString(4, novelId.value());
            statement.setString(5, request.expectedHead().revisionId().value());
            statement.setString(6, request.expectedHead().contentHash());
            if (statement.executeUpdate() != 1) {
                throw new StaleHeadException(
                        request.expectedHead(), requireHead(connection, novelId)
                );
            }
        }
        SqliteSecurityStore.insertAuditEvent(connection, commitAuditEvent(
                novelId,
                request.operation().context().operationId(),
                request.candidate().id(),
                request.operationHash(),
                request.candidateHash(),
                AuditResult.SUCCEEDED,
                auditContext
        ));
        faultInjector.after(CommitStage.AFTER_AUDIT);
        return new CommitResult(
                new RevisionRef(request.candidate().id(), sequence, request.candidateHash()),
                request.operation().context().operationId(),
                false
        );
    }

    private static AuditEvent commitAuditEvent(
            Ids.NovelId novelId,
            Ids.OperationId operationId,
            Ids.RevisionId revisionId,
            String operationHash,
            String contentHash,
            AuditResult result,
            AuditContext context
    ) {
        return AuditEvent.create(
                context,
                novelId,
                AuditAction.COMMIT,
                operationId.value(),
                operationId,
                revisionId,
                result,
                operationHash,
                contentHash
        );
    }

    private static void verifyRequestHashes(CommitRequest request) {
        String operationHash = EditOperationCanonicalMapper.hash(request.operation());
        if (!operationHash.equals(request.operationHash())) {
            throw new IllegalArgumentException("Commit operation hash does not match canonical payload");
        }
        String candidateHash = NarrativeCanonicalMapper.toCanonical(request.candidate()).contentHash();
        if (!candidateHash.equals(request.candidateHash())) {
            throw new IllegalArgumentException("Commit candidate hash does not match canonical content");
        }
    }

    private static void insertOperation(
            Connection connection,
            CommitRequest request,
            long sequence,
            byte[] operationBytes
    ) throws SQLException {
        EditOperation operation = request.operation();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO operations(
                    operation_id, novel_id, sequence, base_revision_id, operation_type,
                    operation_hash, idempotency_key, payload_json, result_revision_id,
                    result_hash, committed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, operation.context().operationId().value());
            statement.setString(2, operation.context().novelId().value());
            statement.setLong(3, sequence);
            statement.setString(4, operation.context().baseRevisionId().value());
            statement.setString(5, operation.type().canonicalName());
            statement.setString(6, request.operationHash());
            statement.setString(7, operation.context().idempotencyKey());
            statement.setString(8, new String(operationBytes, StandardCharsets.UTF_8));
            statement.setString(9, request.candidate().id().value());
            statement.setString(10, request.candidateHash());
            statement.setString(11, request.candidate().createdAt().toString());
            statement.executeUpdate();
        }
    }

    private static void insertRevision(
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

    private static void insertCheckpoint(
            Connection connection,
            RevisionManifest revision,
            long sequence,
            String contentHash,
            byte[] canonicalJson
    ) throws SQLException {
        byte[] compressed = GzipCheckpointCodec.compress(canonicalJson);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO checkpoints(
                    novel_id, revision_id, sequence, content_hash, codec,
                    uncompressed_bytes, compressed_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, revision.novel().id().value());
            statement.setString(2, revision.id().value());
            statement.setLong(3, sequence);
            statement.setString(4, contentHash);
            statement.setString(5, GzipCheckpointCodec.NAME);
            statement.setInt(6, canonicalJson.length);
            statement.setBytes(7, compressed);
            statement.setString(8, revision.createdAt().toString());
            statement.executeUpdate();
        }
    }

    private static void rebuildProjection(
            Connection connection,
            RevisionManifest revision
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM head_block_projection WHERE novel_id = ?"
        )) {
            delete.setString(1, revision.novel().id().value());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO head_block_projection(
                    novel_id, chapter_id, scene_id, block_id, block_version_id,
                    order_key, text_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (NarrativeChapter chapter : revision.novel().chapters()) {
                for (NarrativeScene scene : chapter.scenes()) {
                    for (NarrativeBlock block : scene.blocks()) {
                        insert.setString(1, revision.novel().id().value());
                        insert.setString(2, chapter.id().value());
                        insert.setString(3, scene.id().value());
                        insert.setString(4, block.id().value());
                        insert.setString(5, block.versionId().value());
                        insert.setString(6, block.orderKey().value());
                        insert.setString(7, CanonicalJson.hash(block.text()));
                        insert.addBatch();
                    }
                }
            }
            insert.executeBatch();
        }
    }

    private void insertTombstones(
            Connection connection,
            CommitRequest request
    ) throws SQLException {
        StoredRevision base = getRevision(connection,
                request.operation().context().novelId(), request.expectedHead().revisionId());
        Map<Ids.BlockId, LocatedBlock> previous = locateBlocks(base.manifest());
        Map<Ids.BlockId, LocatedBlock> current = locateBlocks(request.candidate());
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
                statement.setString(7, CanonicalJson.string(blockToMap(deleted.block())));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private boolean shouldCheckpoint(
            Connection connection,
            Ids.NovelId novelId,
            long currentSequence
    ) throws SQLException {
        long checkpointSequence = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(sequence), 0) FROM checkpoints WHERE novel_id = ?"
        )) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                checkpointSequence = result.getLong(1);
            }
        }
        if (currentSequence - checkpointSequence >= checkpointPolicy.revisionInterval()) {
            return true;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(SUM(length(CAST(payload_json AS BLOB))), 0)
                FROM operations
                WHERE novel_id = ? AND sequence > ? AND sequence <= ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setLong(2, checkpointSequence);
            statement.setLong(3, currentSequence);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) >= checkpointPolicy.replayBytesThreshold();
            }
        }
    }

    private static Optional<StoredOperation> findByIdempotencyKey(
            Connection connection,
            Ids.NovelId novelId,
            String key
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT operation_id, novel_id, base_revision_id, operation_type,
                       idempotency_key, sequence, operation_hash, payload_json,
                       result_revision_id, result_hash, committed_at
                FROM operations
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readOperation(result)) : Optional.empty();
            }
        }
    }

    private static StoredOperation readOperation(ResultSet result) throws SQLException {
        EditOperation operation = EditOperationCanonicalMapper.fromCanonical(
                result.getString("payload_json").getBytes(StandardCharsets.UTF_8)
        );
        String storedHash = result.getString("operation_hash");
        if (!EditOperationCanonicalMapper.hash(operation).equals(storedHash)) {
            throw new StorageException(
                    "Stored operation hash does not match " + operation.context().operationId().value()
            );
        }
        if (!operation.context().operationId().value().equals(result.getString("operation_id"))
                || !operation.context().novelId().value().equals(result.getString("novel_id"))
                || !operation.context().baseRevisionId().value().equals(
                        result.getString("base_revision_id")
                )
                || !operation.type().canonicalName().equals(result.getString("operation_type"))
                || !operation.context().idempotencyKey().equals(
                        result.getString("idempotency_key")
                )) {
            throw new StorageException("Stored operation relational identity does not match payload");
        }
        return new StoredOperation(
                operation,
                result.getLong("sequence"),
                storedHash,
                new Ids.RevisionId(result.getString("result_revision_id")),
                result.getString("result_hash"),
                Instant.parse(result.getString("committed_at"))
        );
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
                    throw new MissingNovelException(novelId);
                }
                return new RevisionRef(
                        new Ids.RevisionId(result.getString("head_revision_id")),
                        result.getLong("head_sequence"),
                        result.getString("head_hash")
                );
            }
        }
    }

    private static StoredRevision getRevision(
            Connection connection,
            Ids.NovelId novelId,
            Ids.RevisionId revisionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT parent_revision_id, sequence, content_hash,
                       canonical_json, created_at
                FROM revisions
                WHERE novel_id = ? AND revision_id = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, revisionId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingRevisionException(novelId, revisionId);
                }
                return readRevision(result, novelId, revisionId);
            }
        }
    }

    private static StoredRevision readRevision(
            ResultSet result,
            Ids.NovelId novelId,
            Ids.RevisionId revisionId
    ) throws SQLException {
        String storedHash = result.getString("content_hash");
        CanonicalRevision canonical = CanonicalRevision.parseEnvelope(result.getBytes("canonical_json"));
        if (!canonical.contentHash().equals(storedHash)) {
            throw new StorageException("Stored revision hash does not match " + revisionId.value());
        }
        RevisionManifest manifest = NarrativeCanonicalMapper.fromCanonical(canonical);
        if (!manifest.novel().id().equals(novelId) || !manifest.id().equals(revisionId)) {
            throw new StorageException("Stored revision identity does not match relational columns");
        }
        String relationalParent = result.getString("parent_revision_id");
        String canonicalParent = manifest.parentId() == null ? null : manifest.parentId().value();
        if (!Objects.equals(relationalParent, canonicalParent)
                || !manifest.createdAt().toString().equals(result.getString("created_at"))) {
            throw new StorageException("Stored revision lineage does not match canonical content");
        }
        return new StoredRevision(manifest, result.getLong("sequence"), storedHash);
    }

    private long count(Ids.NovelId novelId, String table) {
        if (!table.equals("revisions") && !table.equals("operations")) {
            throw new IllegalArgumentException("Unsupported count table");
        }
        return read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE novel_id = ?"
            )) {
                statement.setString(1, novelId.value());
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return result.getLong(1);
                }
            }
        });
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

    private static NarrativeBlock parseBlock(String json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> value = CanonicalJson.mapper().readValue(json, Map.class);
        return new NarrativeBlock(
                new Ids.BlockId(requiredString(value, "id")),
                new Ids.BlockVersionId(requiredString(value, "block_version_id")),
                new OrderKey(requiredString(value, "order_key")),
                requiredString(value, "text"),
                new BlockMetadata(requiredMap(value, "meta")),
                value.containsKey("extensions") ? requiredMap(value, "extensions") : Map.of()
        );
    }

    private static String requiredString(Map<String, Object> value, String field) {
        Object entry = value.get(field);
        if (!(entry instanceof String string)) {
            throw new StorageException("Stored tombstone field " + field + " is not a string");
        }
        return string;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requiredMap(Map<String, Object> value, String field) {
        Object entry = value.get(field);
        if (!(entry instanceof Map<?, ?> map)) {
            throw new StorageException("Stored tombstone field " + field + " is not an object");
        }
        return (Map<String, Object>) map;
    }

    private <T> T read(SqliteWork<T> work) {
        try {
            return database.readOnly(work);
        } catch (SQLException exception) {
            throw new StorageException("SQLite revision read failed", exception);
        }
    }

    private <T> T write(SqliteWork<T> work) {
        try {
            return database.write(work);
        } catch (SQLException exception) {
            throw new StorageException("SQLite revision write failed", exception);
        }
    }

    private record LocatedBlock(Ids.SceneId sceneId, NarrativeBlock block) {
    }
}

package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.MissingStyleAnalysisException;
import dev.storyblock.style.MissingStyleAnalysisJobException;
import dev.storyblock.style.StyleAnalysisClaimCommand;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisCompletionResult;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobSaveResult;
import dev.storyblock.style.StyleAnalysisJobStatus;
import dev.storyblock.style.StyleAnalysisLease;
import dev.storyblock.style.StyleAnalysisLeaseConflictException;
import dev.storyblock.style.StyleAnalysisResult;
import dev.storyblock.style.StyleAnalysisResultConflictException;
import dev.storyblock.style.StyleAnalysisSnapshot;
import dev.storyblock.style.StyleAnalysisSummary;
import dev.storyblock.style.StyleAnalysisTrace;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleAnalysisWindowSlice;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class SqliteStyleAnalysisStore {
    private static final String JOB_COLUMNS = """
            job_id, analysis_id, novel_id, revision_id, revision_hash,
            profile_id, profile_version_id, profile_version_hash,
            analyzer_contract_hash, window_configuration_hash, snapshot_hash,
            snapshot_json, status, lease_owner, lease_until, attempt, max_attempts,
            idempotency_key, request_hash, result_artifact_id, result_hash,
            failure_code, request_id, actor_id, actor_key_id, retention_until,
            created_at, updated_at
            """;

    private SqliteStyleAnalysisStore() {
    }

    static StyleAnalysisJobSaveResult createJob(
            Connection connection,
            StyleAnalysisJob job
    ) throws SQLException {
        Optional<StyleAnalysisJob> prior = findByIdempotencyKey(
                connection, job.snapshot().novelId(), job.idempotencyKey()
        );
        if (prior.isPresent()) {
            StyleAnalysisJob stored = prior.get();
            if (!stored.requestHash().equals(job.requestHash())) {
                throw new IdempotencyConflictException(
                        job.idempotencyKey(), stored.requestHash(), job.requestHash()
                );
            }
            return new StyleAnalysisJobSaveResult(stored, true);
        }
        requireSnapshotVersions(connection, job.snapshot());
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_jobs(
                    job_id, analysis_id, novel_id, revision_id, revision_hash,
                    profile_id, profile_version_id, profile_version_hash,
                    analyzer_contract_hash, window_configuration_hash, snapshot_hash,
                    snapshot_json, status, lease_owner, lease_until, attempt, max_attempts,
                    idempotency_key, request_hash, result_artifact_id, result_hash,
                    failure_code, request_id, actor_id, actor_key_id, retention_until,
                    created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, 0, ?, ?, ?,
                    NULL, NULL, NULL, ?, ?, ?, ?, ?, ?
                )
                """)) {
            StyleAnalysisSnapshot snapshot = job.snapshot();
            statement.setString(1, job.jobId().value());
            statement.setString(2, job.analysisId().value());
            statement.setString(3, snapshot.novelId().value());
            statement.setString(4, snapshot.revisionId().value());
            statement.setString(5, snapshot.revisionHash());
            statement.setString(6, snapshot.profileVersion().profileId().value());
            statement.setString(7, snapshot.profileVersion().versionId().value());
            statement.setString(8, snapshot.profileVersionHash());
            statement.setString(9, snapshot.analyzerContractHash());
            statement.setString(10, snapshot.windowConfigurationHash());
            statement.setString(11, snapshot.snapshotHash());
            statement.setString(12, CanonicalJson.string(snapshot.canonicalValue()));
            statement.setString(13, job.status().canonicalName());
            statement.setInt(14, job.maxAttempts());
            statement.setString(15, job.idempotencyKey());
            statement.setString(16, job.requestHash());
            statement.setString(17, job.auditContext().requestId());
            statement.setString(18, job.auditContext().actorId());
            nullableString(
                    statement,
                    19,
                    job.auditContext().actorKeyId() == null
                            ? null : job.auditContext().actorKeyId().value()
            );
            statement.setString(20, job.retentionUntil().toString());
            statement.setString(21, job.createdAt().toString());
            statement.setString(22, job.updatedAt().toString());
            statement.executeUpdate();
        }
        return new StyleAnalysisJobSaveResult(job, false);
    }

    static StyleAnalysisJob getJob(Connection connection, Ids.JobId jobId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs WHERE job_id = ?"
        )) {
            statement.setString(1, jobId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleAnalysisJobException(jobId);
                }
                return readJob(result);
            }
        }
    }

    static StyleAnalysisJob getAnalysis(
            Connection connection,
            Ids.StyleAnalysisId analysisId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs WHERE analysis_id = ?"
        )) {
            statement.setString(1, analysisId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleAnalysisException(analysisId);
                }
                return readJob(result);
            }
        }
    }

    static Optional<StyleAnalysisLease> claim(
            Connection connection,
            StyleAnalysisClaimCommand command
    ) throws SQLException {
        Optional<ClaimReceipt> prior = findClaimReceipt(
                connection, command.novelId(), command.idempotencyKey()
        );
        if (prior.isPresent()) {
            ClaimReceipt receipt = prior.get();
            if (!receipt.requestHash().equals(command.requestHash())) {
                throw new IdempotencyConflictException(
                        command.idempotencyKey(),
                        receipt.requestHash(),
                        command.requestHash()
                );
            }
            return receipt.jobId() == null
                    ? Optional.empty()
                    : Optional.of(new StyleAnalysisLease(
                            receipt.jobId(),
                            receipt.analysisId(),
                            receipt.snapshot(),
                            receipt.leaseOwner(),
                            receipt.attempt(),
                            receipt.leaseUntil(),
                            receipt.retentionUntil(),
                            receipt.claimedStatusHash(),
                            true
                    ));
        }

        failExhaustedLeases(connection, command.claimedAt());
        Optional<StyleAnalysisJob> candidate = findClaimCandidate(
                connection, command.novelId(), command.claimedAt()
        );
        if (candidate.isEmpty()) {
            insertClaimReceipt(connection, command, null);
            return Optional.empty();
        }

        StyleAnalysisJob current = candidate.get();
        Instant leaseUntil = command.claimedAt().plus(command.leaseDuration());
        int attempt = current.attempt() + 1;
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'running', lease_owner = ?, lease_until = ?, attempt = ?,
                    updated_at = ?, failure_code = NULL
                WHERE job_id = ?
                  AND attempt = ?
                  AND (
                      status = 'queued'
                      OR (status = 'running' AND julianday(lease_until) <= julianday(?))
                  )
                """)) {
            statement.setString(1, command.leaseOwner());
            statement.setString(2, leaseUntil.toString());
            statement.setInt(3, attempt);
            statement.setString(4, command.claimedAt().toString());
            statement.setString(5, current.jobId().value());
            statement.setInt(6, current.attempt());
            statement.setString(7, command.claimedAt().toString());
            if (statement.executeUpdate() != 1) {
                throw new StorageException("Style analysis claim lost its fencing race");
            }
        }
        StyleAnalysisJob claimed = getJob(connection, current.jobId());
        StyleAnalysisLease lease = new StyleAnalysisLease(
                claimed.jobId(),
                claimed.analysisId(),
                claimed.snapshot(),
                command.leaseOwner(),
                attempt,
                leaseUntil,
                claimed.retentionUntil(),
                claimed.statusHash(),
                false
        );
        insertClaimReceipt(connection, command, lease);
        return Optional.of(lease);
    }

    static StyleAnalysisCompletionResult complete(
            Connection connection,
            StyleAnalysisCompletionCommand command
    ) throws SQLException {
        Optional<StoredResult> prior = findResultByJob(connection, command.jobId());
        if (prior.isPresent()) {
            StoredResult stored = prior.get();
            if (!stored.result().resultHash().equals(command.resultHash())) {
                throw new StyleAnalysisResultConflictException(
                        stored.result().resultHash(), command.resultHash()
                );
            }
            if (stored.idempotencyKey().equals(command.idempotencyKey())
                    && !stored.requestHash().equals(command.requestHash())) {
                throw new IdempotencyConflictException(
                        command.idempotencyKey(),
                        stored.requestHash(),
                        command.requestHash()
                );
            }
            return new StyleAnalysisCompletionResult(
                    getJob(connection, command.jobId()), stored.result(), true
            );
        }

        StyleAnalysisJob job = getJob(connection, command.jobId());
        requireActiveLease(job, command);
        requireCompletionVersions(job, command);
        insertTraceArtifact(connection, job, command.trace());
        insertRun(connection, job, command);
        insertWindows(connection, job.analysisId(), command.windows());
        insertAnalysisArtifact(connection, command.trace());
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'succeeded', lease_owner = NULL, lease_until = NULL,
                    result_artifact_id = ?, result_hash = ?, updated_at = ?
                WHERE job_id = ? AND status = 'running'
                  AND lease_owner = ? AND attempt = ?
                """)) {
            statement.setString(1, command.trace().artifactId().value());
            statement.setString(2, command.resultHash());
            statement.setString(3, command.completedAt().toString());
            statement.setString(4, command.jobId().value());
            statement.setString(5, command.leaseOwner());
            statement.setInt(6, command.attempt());
            if (statement.executeUpdate() != 1) {
                throw new StyleAnalysisLeaseConflictException(
                        "Style analysis lease changed before result commit",
                        getJob(connection, command.jobId()).statusHash()
                );
            }
        }
        StyleAnalysisJob completed = getJob(connection, command.jobId());
        StyleAnalysisResult result = findResultByJob(connection, command.jobId())
                .orElseThrow(() -> new StorageException(
                        "Committed style analysis result cannot be read"
                )).result();
        return new StyleAnalysisCompletionResult(completed, result, false);
    }

    static StyleAnalysisJob fail(
            Connection connection,
            Ids.JobId jobId,
            String leaseOwner,
            int attempt,
            String expectedStatusHash,
            String failureCode,
            Instant failedAt
    ) throws SQLException {
        StyleAnalysisJob job = getJob(connection, jobId);
        if (job.status() != StyleAnalysisJobStatus.RUNNING
                || !job.statusHash().equals(expectedStatusHash)
                || !Objects.equals(job.leaseOwner(), leaseOwner)
                || job.attempt() != attempt
                || failedAt.isBefore(job.updatedAt())
                || !failedAt.isBefore(job.leaseUntil())) {
            throw new StyleAnalysisLeaseConflictException(
                    "Style analysis failure submission does not own the active lease",
                    job.statusHash()
            );
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'failed', lease_owner = NULL, lease_until = NULL,
                    failure_code = ?, updated_at = ?
                WHERE job_id = ? AND status = 'running'
                  AND lease_owner = ? AND attempt = ?
                """)) {
            statement.setString(1, failureCode);
            statement.setString(2, failedAt.toString());
            statement.setString(3, jobId.value());
            statement.setString(4, leaseOwner);
            statement.setInt(5, attempt);
            if (statement.executeUpdate() != 1) {
                throw new StyleAnalysisLeaseConflictException(
                        "Style analysis lease changed before failure commit",
                        getJob(connection, jobId).statusHash()
                );
            }
        }
        return getJob(connection, jobId);
    }

    static Optional<StyleAnalysisResult> findResult(
            Connection connection,
            Ids.StyleAnalysisId analysisId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT analysis_id, job_id, summary_json, result_artifact_id,
                       trace_content_hash, trace_uncompressed_bytes, trace_expires_at,
                       result_hash, completed_at
                FROM analysis_runs
                WHERE analysis_id = ?
                """)) {
            statement.setString(1, analysisId.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(readResult(result)) : Optional.empty();
            }
        }
    }

    static StyleAnalysisWindowSlice listWindows(
            Connection connection,
            Ids.StyleAnalysisId analysisId,
            int afterOrdinal,
            int limit
    ) throws SQLException {
        if (afterOrdinal < -1 || limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Style analysis page bounds are invalid");
        }
        getAnalysis(connection, analysisId);
        List<StyleAnalysisWindowFinding> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT ordinal, window_id, decision_state, can_trigger_rewrite,
                       payload_json
                FROM analysis_window_findings
                WHERE analysis_id = ? AND ordinal > ?
                ORDER BY ordinal
                LIMIT ?
                """)) {
            statement.setString(1, analysisId.value());
            statement.setInt(2, afterOrdinal);
            statement.setInt(3, limit + 1);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    StyleAnalysisWindowFinding finding = StyleAnalysisWindowFinding
                            .fromCanonical(parseObject(
                                    result.getString("payload_json"),
                                    "style analysis window"
                            ));
                    if (finding.ordinal() != result.getInt("ordinal")
                            || !finding.windowId().equals(result.getString("window_id"))
                            || !finding.decisionState().canonicalName().equals(
                                    result.getString("decision_state")
                            )
                            || finding.canTriggerRewrite()
                            != result.getBoolean("can_trigger_rewrite")) {
                        throw new StorageException(
                                "Stored style analysis window integrity check failed"
                        );
                    }
                    values.add(finding);
                }
            }
        }
        Integer next = null;
        if (values.size() > limit) {
            values.removeLast();
            next = values.getLast().ordinal();
        }
        return new StyleAnalysisWindowSlice(values, next);
    }

    static Optional<Instant> findArtifactExpiry(
            Connection connection,
            Ids.ArtifactId artifactId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT expires_at
                FROM analysis_artifacts
                WHERE artifact_id = ?
                """)) {
            statement.setString(1, artifactId.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(Instant.parse(result.getString(1)))
                        : Optional.empty();
            }
        }
    }

    private static Optional<StyleAnalysisJob> findByIdempotencyKey(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs "
                        + "WHERE novel_id = ? AND idempotency_key = ?"
        )) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readJob(result)) : Optional.empty();
            }
        }
    }

    private static StyleAnalysisJob readJob(ResultSet result) throws SQLException {
        StyleAnalysisSnapshot snapshot = StyleAnalysisSnapshot.fromCanonical(parseObject(
                result.getString("snapshot_json"), "style analysis snapshot"
        ));
        String actorKey = result.getString("actor_key_id");
        AuditContext audit = new AuditContext(
                result.getString("request_id"),
                result.getString("actor_id"),
                actorKey == null ? null : new Ids.AccessKeyId(actorKey),
                Instant.parse(result.getString("created_at"))
        );
        StyleAnalysisJob job = new StyleAnalysisJob(
                new Ids.JobId(result.getString("job_id")),
                new Ids.StyleAnalysisId(result.getString("analysis_id")),
                snapshot,
                StyleAnalysisJobStatus.fromCanonicalName(result.getString("status")),
                result.getString("lease_owner"),
                nullableInstant(result.getString("lease_until")),
                result.getInt("attempt"),
                result.getInt("max_attempts"),
                result.getString("idempotency_key"),
                result.getString("request_hash"),
                nullableArtifact(result.getString("result_artifact_id")),
                result.getString("result_hash"),
                result.getString("failure_code"),
                audit,
                Instant.parse(result.getString("retention_until")),
                audit.occurredAt(),
                Instant.parse(result.getString("updated_at"))
        );
        if (!snapshot.novelId().value().equals(result.getString("novel_id"))
                || !snapshot.revisionId().value().equals(result.getString("revision_id"))
                || !snapshot.revisionHash().equals(result.getString("revision_hash"))
                || !snapshot.profileVersion().profileId().value().equals(
                        result.getString("profile_id")
                )
                || !snapshot.profileVersion().versionId().value().equals(
                        result.getString("profile_version_id")
                )
                || !snapshot.profileVersionHash().equals(
                        result.getString("profile_version_hash")
                )
                || !snapshot.analyzerContractHash().equals(
                        result.getString("analyzer_contract_hash")
                )
                || !snapshot.windowConfigurationHash().equals(
                        result.getString("window_configuration_hash")
                )
                || !snapshot.snapshotHash().equals(result.getString("snapshot_hash"))) {
            throw new StorageException("Stored style analysis snapshot integrity check failed");
        }
        return job;
    }

    private static void requireSnapshotVersions(
            Connection connection,
            StyleAnalysisSnapshot snapshot
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT revision.content_hash, version.version_hash
                FROM revisions AS revision
                JOIN style_profile_versions AS version
                  ON version.profile_id = ? AND version.version_id = ?
                WHERE revision.novel_id = ? AND revision.revision_id = ?
                """)) {
            statement.setString(1, snapshot.profileVersion().profileId().value());
            statement.setString(2, snapshot.profileVersion().versionId().value());
            statement.setString(3, snapshot.novelId().value());
            statement.setString(4, snapshot.revisionId().value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()
                        || !snapshot.revisionHash().equals(result.getString(1))
                        || !snapshot.profileVersionHash().equals(result.getString(2))) {
                    throw new StorageException(
                            "Style analysis snapshot versions do not match canonical storage"
                    );
                }
            }
        }
    }

    private static void failExhaustedLeases(Connection connection, Instant now)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE analysis_jobs
                SET status = 'failed', lease_owner = NULL, lease_until = NULL,
                    failure_code = 'attempts_exhausted', updated_at = ?
                WHERE status = 'running'
                  AND julianday(lease_until) <= julianday(?)
                  AND attempt >= max_attempts
                """)) {
            statement.setString(1, now.toString());
            statement.setString(2, now.toString());
            statement.executeUpdate();
        }
    }

    private static Optional<StyleAnalysisJob> findClaimCandidate(
            Connection connection,
            Ids.NovelId novelId,
            Instant now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + JOB_COLUMNS + " FROM analysis_jobs " + """
                        WHERE novel_id = ? AND attempt < max_attempts
                          AND (
                              status = 'queued'
                              OR (status = 'running'
                                  AND julianday(lease_until) <= julianday(?))
                          )
                        ORDER BY created_at, job_id
                        LIMIT 1
                        """
        )) {
            statement.setString(1, novelId.value());
            statement.setString(2, now.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readJob(result)) : Optional.empty();
            }
        }
    }

    private static Optional<ClaimReceipt> findClaimReceipt(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT receipt.request_hash, receipt.job_id, receipt.lease_owner,
                       receipt.attempt, receipt.lease_until,
                       receipt.claimed_status_hash, job.analysis_id, job.snapshot_json,
                       job.retention_until
                FROM analysis_claim_receipts AS receipt
                LEFT JOIN analysis_jobs AS job ON job.job_id = receipt.job_id
                WHERE receipt.novel_id = ? AND receipt.idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String job = result.getString("job_id");
                return Optional.of(new ClaimReceipt(
                        result.getString("request_hash"),
                        job == null ? null : new Ids.JobId(job),
                        job == null ? null : new Ids.StyleAnalysisId(
                                result.getString("analysis_id")
                        ),
                        job == null ? null : StyleAnalysisSnapshot.fromCanonical(
                                parseObject(
                                        result.getString("snapshot_json"),
                                        "style claim snapshot"
                                )
                        ),
                        result.getString("lease_owner"),
                        job == null ? 0 : result.getInt("attempt"),
                        nullableInstant(result.getString("lease_until")),
                        nullableInstant(result.getString("retention_until")),
                        result.getString("claimed_status_hash")
                ));
            }
        }
    }

    private static void insertClaimReceipt(
            Connection connection,
            StyleAnalysisClaimCommand command,
            StyleAnalysisLease lease
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_claim_receipts(
                    novel_id, idempotency_key, request_hash, job_id, lease_owner,
                    attempt, lease_until, claimed_status_hash, claimed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, command.novelId().value());
            statement.setString(2, command.idempotencyKey());
            statement.setString(3, command.requestHash());
            nullableString(statement, 4, lease == null ? null : lease.jobId().value());
            statement.setString(5, command.leaseOwner());
            if (lease == null) {
                statement.setNull(6, Types.INTEGER);
            } else {
                statement.setInt(6, lease.attempt());
            }
            nullableString(
                    statement, 7, lease == null ? null : lease.leaseUntil().toString()
            );
            nullableString(
                    statement,
                    8,
                    lease == null ? null : lease.claimedStatusHash()
            );
            statement.setString(9, command.claimedAt().toString());
            statement.executeUpdate();
        }
    }

    private static void requireActiveLease(
            StyleAnalysisJob job,
            StyleAnalysisCompletionCommand command
    ) {
        if (job.status() != StyleAnalysisJobStatus.RUNNING
                || !job.statusHash().equals(command.expectedStatusHash())
                || !Objects.equals(job.leaseOwner(), command.leaseOwner())
                || job.attempt() != command.attempt()
                || command.completedAt().isBefore(job.updatedAt())
                || !command.completedAt().isBefore(job.leaseUntil())) {
            throw new StyleAnalysisLeaseConflictException(
                    "Style analysis result does not own the active lease",
                    job.statusHash()
            );
        }
    }

    private static void requireCompletionVersions(
            StyleAnalysisJob job,
            StyleAnalysisCompletionCommand command
    ) {
        StyleAnalysisSnapshot snapshot = job.snapshot();
        if (!snapshot.snapshotHash().equals(command.snapshotHash())
                || !snapshot.profileVersionHash().equals(command.profileVersionHash())
                || !snapshot.analyzerContractHash().equals(
                        command.analyzerContractHash()
                )
                || !snapshot.windowConfigurationHash().equals(
                        command.windowConfigurationHash()
                )
                || !job.analysisId().equals(command.trace().analysisId())
                || !job.retentionUntil().equals(command.trace().expiresAt())) {
            throw new StyleAnalysisLeaseConflictException(
                    "Style analysis result versions do not match the leased snapshot",
                    job.statusHash()
            );
        }
    }

    private static void insertTraceArtifact(
            Connection connection,
            StyleAnalysisJob job,
            StyleAnalysisTrace trace
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO artifacts(
                    artifact_id, novel_id, revision_id, kind, media_type, codec,
                    content_hash, size_bytes, content, created_at, portable
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """)) {
            statement.setString(1, trace.artifactId().value());
            statement.setString(2, job.snapshot().novelId().value());
            statement.setString(3, job.snapshot().revisionId().value());
            statement.setString(4, StyleAnalysisTrace.KIND);
            statement.setString(5, StyleAnalysisTrace.MEDIA_TYPE);
            statement.setString(6, StyleAnalysisTrace.CODEC);
            statement.setString(7, trace.contentHash());
            statement.setInt(8, trace.compressedContent().length);
            statement.setBytes(9, trace.compressedContent());
            statement.setString(10, trace.createdAt().toString());
            statement.executeUpdate();
        }
    }

    private static void insertRun(
            Connection connection,
            StyleAnalysisJob job,
            StyleAnalysisCompletionCommand command
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_runs(
                    analysis_id, job_id, summary_json, result_hash,
                    result_artifact_id, trace_content_hash, trace_uncompressed_bytes,
                    trace_expires_at, submission_idempotency_key,
                    submission_request_hash, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, job.analysisId().value());
            statement.setString(2, job.jobId().value());
            statement.setString(3, CanonicalJson.string(command.summary().canonicalValue()));
            statement.setString(4, command.resultHash());
            statement.setString(5, command.trace().artifactId().value());
            statement.setString(6, command.trace().contentHash());
            statement.setInt(7, command.trace().uncompressedBytes());
            statement.setString(8, command.trace().expiresAt().toString());
            statement.setString(9, command.idempotencyKey());
            statement.setString(10, command.requestHash());
            statement.setString(11, command.completedAt().toString());
            statement.executeUpdate();
        }
    }

    private static void insertWindows(
            Connection connection,
            Ids.StyleAnalysisId analysisId,
            List<StyleAnalysisWindowFinding> windows
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_window_findings(
                    analysis_id, ordinal, window_id, decision_state,
                    can_trigger_rewrite, payload_json
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (StyleAnalysisWindowFinding window : windows) {
                statement.setString(1, analysisId.value());
                statement.setInt(2, window.ordinal());
                statement.setString(3, window.windowId());
                statement.setString(4, window.decisionState().canonicalName());
                statement.setBoolean(5, window.canTriggerRewrite());
                statement.setString(6, CanonicalJson.string(window.canonicalValue()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertAnalysisArtifact(
            Connection connection,
            StyleAnalysisTrace trace
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_artifacts(
                    artifact_id, analysis_id, expires_at, uncompressed_bytes
                ) VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, trace.artifactId().value());
            statement.setString(2, trace.analysisId().value());
            statement.setString(3, trace.expiresAt().toString());
            statement.setInt(4, trace.uncompressedBytes());
            statement.executeUpdate();
        }
    }

    private static Optional<StoredResult> findResultByJob(
            Connection connection,
            Ids.JobId jobId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT analysis_id, job_id, summary_json, result_artifact_id,
                       trace_content_hash, trace_uncompressed_bytes, trace_expires_at,
                       result_hash, completed_at, submission_idempotency_key,
                       submission_request_hash
                FROM analysis_runs
                WHERE job_id = ?
                """)) {
            statement.setString(1, jobId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StoredResult(
                        readResult(result),
                        result.getString("submission_idempotency_key"),
                        result.getString("submission_request_hash")
                ));
            }
        }
    }

    private static StyleAnalysisResult readResult(ResultSet result) throws SQLException {
        return new StyleAnalysisResult(
                new Ids.StyleAnalysisId(result.getString("analysis_id")),
                new Ids.JobId(result.getString("job_id")),
                StyleAnalysisSummary.fromCanonical(parseObject(
                        result.getString("summary_json"), "style analysis summary"
                )),
                new Ids.ArtifactId(result.getString("result_artifact_id")),
                result.getString("trace_content_hash"),
                result.getInt("trace_uncompressed_bytes"),
                Instant.parse(result.getString("trace_expires_at")),
                result.getString("result_hash"),
                Instant.parse(result.getString("completed_at"))
        );
    }

    private static Map<String, Object> parseObject(String json, String path) {
        Object parsed = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), Map.class
        );
        if (!(parsed instanceof Map<?, ?> raw)) {
            throw new StorageException("Stored " + path + " is not an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new StorageException("Stored " + path + " has a non-string key");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static void nullableString(
            PreparedStatement statement,
            int index,
            String value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static Instant nullableInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private static Ids.ArtifactId nullableArtifact(String value) {
        return value == null ? null : new Ids.ArtifactId(value);
    }

    private record ClaimReceipt(
            String requestHash,
            Ids.JobId jobId,
            Ids.StyleAnalysisId analysisId,
            StyleAnalysisSnapshot snapshot,
            String leaseOwner,
            int attempt,
            Instant leaseUntil,
            Instant retentionUntil,
            String claimedStatusHash
    ) {
    }

    private record StoredResult(
            StyleAnalysisResult result,
            String idempotencyKey,
            String requestHash
    ) {
    }
}

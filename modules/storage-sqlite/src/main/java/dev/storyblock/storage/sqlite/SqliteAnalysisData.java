package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisResult;
import dev.storyblock.style.StyleAnalysisSnapshot;
import java.time.Instant;

final class SqliteAnalysisData {
    static final String JOB_COLUMNS = """
            job_id, analysis_id, novel_id, revision_id, revision_hash,
            profile_id, profile_version_id, profile_version_hash,
            analyzer_contract_hash, window_configuration_hash, snapshot_hash,
            snapshot_json, status, lease_owner, lease_until, attempt, max_attempts,
            idempotency_key, request_hash, result_artifact_id, result_hash,
            failure_code, request_id, actor_id, actor_key_id, retention_until,
            created_at, updated_at
            """;

    record ClaimReceipt(
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

    record StoredResult(
            StyleAnalysisResult result,
            String idempotencyKey,
            String requestHash
    ) {
    }
}

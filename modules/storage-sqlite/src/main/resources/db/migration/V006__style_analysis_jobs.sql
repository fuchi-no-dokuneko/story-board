CREATE TABLE IF NOT EXISTS analysis_jobs (
    job_id TEXT PRIMARY KEY,
    analysis_id TEXT NOT NULL UNIQUE,
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    revision_hash TEXT NOT NULL CHECK (
        length(revision_hash) = 71
        AND substr(revision_hash, 1, 7) = 'sha256:'
        AND substr(revision_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    profile_id TEXT NOT NULL,
    profile_version_id TEXT NOT NULL,
    profile_version_hash TEXT NOT NULL CHECK (
        length(profile_version_hash) = 71
        AND substr(profile_version_hash, 1, 7) = 'sha256:'
        AND substr(profile_version_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    analyzer_contract_hash TEXT NOT NULL CHECK (
        length(analyzer_contract_hash) = 71
        AND substr(analyzer_contract_hash, 1, 7) = 'sha256:'
        AND substr(analyzer_contract_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    window_configuration_hash TEXT NOT NULL CHECK (
        length(window_configuration_hash) = 71
        AND substr(window_configuration_hash, 1, 7) = 'sha256:'
        AND substr(window_configuration_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    snapshot_hash TEXT NOT NULL CHECK (
        length(snapshot_hash) = 71
        AND substr(snapshot_hash, 1, 7) = 'sha256:'
        AND substr(snapshot_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    snapshot_json TEXT NOT NULL CHECK (
        json_valid(snapshot_json) AND json_type(snapshot_json) = 'object'
    ),
    status TEXT NOT NULL CHECK (
        status IN ('queued', 'running', 'succeeded', 'failed')
    ),
    lease_owner TEXT CHECK (
        lease_owner IS NULL OR length(lease_owner) BETWEEN 1 AND 128
    ),
    lease_until TEXT CHECK (
        lease_until IS NULL OR julianday(lease_until) IS NOT NULL
    ),
    attempt INTEGER NOT NULL CHECK (attempt BETWEEN 0 AND 20),
    max_attempts INTEGER NOT NULL CHECK (max_attempts BETWEEN 1 AND 20),
    idempotency_key TEXT NOT NULL CHECK (length(idempotency_key) BETWEEN 1 AND 200),
    request_hash TEXT NOT NULL CHECK (
        length(request_hash) = 71
        AND substr(request_hash, 1, 7) = 'sha256:'
        AND substr(request_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    result_artifact_id TEXT,
    result_hash TEXT CHECK (
        result_hash IS NULL OR (
            length(result_hash) = 71
            AND substr(result_hash, 1, 7) = 'sha256:'
            AND substr(result_hash, 8) NOT GLOB '*[^0-9a-f]*'
        )
    ),
    failure_code TEXT,
    request_id TEXT NOT NULL CHECK (length(request_id) BETWEEN 1 AND 128),
    actor_id TEXT NOT NULL CHECK (length(actor_id) BETWEEN 1 AND 128),
    actor_key_id TEXT,
    retention_until TEXT NOT NULL CHECK (julianday(retention_until) IS NOT NULL),
    created_at TEXT NOT NULL CHECK (julianday(created_at) IS NOT NULL),
    updated_at TEXT NOT NULL CHECK (julianday(updated_at) IS NOT NULL),
    UNIQUE (novel_id, idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id),
    FOREIGN KEY (profile_id, profile_version_id)
        REFERENCES style_profile_versions(profile_id, version_id),
    FOREIGN KEY (result_artifact_id) REFERENCES artifacts(artifact_id)
        DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (actor_key_id) REFERENCES access_keys(key_id),
    CHECK (attempt <= max_attempts),
    CHECK (julianday(retention_until) > julianday(created_at)),
    CHECK (julianday(updated_at) >= julianday(created_at)),
    CHECK (
        (status = 'queued' AND lease_owner IS NULL AND lease_until IS NULL
            AND attempt = 0 AND result_artifact_id IS NULL AND result_hash IS NULL
            AND failure_code IS NULL)
        OR
        (status = 'running' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL
            AND attempt > 0 AND result_artifact_id IS NULL AND result_hash IS NULL
            AND failure_code IS NULL)
        OR
        (status = 'succeeded' AND lease_owner IS NULL AND lease_until IS NULL
            AND attempt > 0 AND result_artifact_id IS NOT NULL AND result_hash IS NOT NULL
            AND failure_code IS NULL)
        OR
        (status = 'failed' AND lease_owner IS NULL AND lease_until IS NULL
            AND attempt > 0 AND result_artifact_id IS NULL AND result_hash IS NULL
            AND failure_code IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS analysis_claim_receipts (
    novel_id TEXT NOT NULL,
    idempotency_key TEXT NOT NULL CHECK (length(idempotency_key) BETWEEN 1 AND 200),
    request_hash TEXT NOT NULL CHECK (
        length(request_hash) = 71
        AND substr(request_hash, 1, 7) = 'sha256:'
        AND substr(request_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    job_id TEXT,
    lease_owner TEXT NOT NULL CHECK (length(lease_owner) BETWEEN 1 AND 128),
    attempt INTEGER,
    lease_until TEXT CHECK (
        lease_until IS NULL OR julianday(lease_until) IS NOT NULL
    ),
    claimed_status_hash TEXT,
    claimed_at TEXT NOT NULL CHECK (julianday(claimed_at) IS NOT NULL),
    PRIMARY KEY (novel_id, idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(job_id),
    CHECK (
        (job_id IS NULL AND attempt IS NULL AND lease_until IS NULL
            AND claimed_status_hash IS NULL)
        OR
        (job_id IS NOT NULL AND attempt > 0 AND lease_until IS NOT NULL
            AND length(claimed_status_hash) = 71
            AND substr(claimed_status_hash, 1, 7) = 'sha256:'
            AND substr(claimed_status_hash, 8) NOT GLOB '*[^0-9a-f]*')
    )
);

CREATE TABLE IF NOT EXISTS analysis_runs (
    analysis_id TEXT PRIMARY KEY,
    job_id TEXT NOT NULL UNIQUE,
    summary_json TEXT NOT NULL CHECK (
        json_valid(summary_json) AND json_type(summary_json) = 'object'
    ),
    result_hash TEXT NOT NULL CHECK (
        length(result_hash) = 71
        AND substr(result_hash, 1, 7) = 'sha256:'
        AND substr(result_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    result_artifact_id TEXT NOT NULL,
    trace_content_hash TEXT NOT NULL CHECK (
        length(trace_content_hash) = 71
        AND substr(trace_content_hash, 1, 7) = 'sha256:'
        AND substr(trace_content_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    trace_uncompressed_bytes INTEGER NOT NULL CHECK (
        trace_uncompressed_bytes BETWEEN 2 AND 16777216
    ),
    trace_expires_at TEXT NOT NULL CHECK (julianday(trace_expires_at) IS NOT NULL),
    submission_idempotency_key TEXT NOT NULL CHECK (
        length(submission_idempotency_key) BETWEEN 1 AND 200
    ),
    submission_request_hash TEXT NOT NULL CHECK (
        length(submission_request_hash) = 71
        AND substr(submission_request_hash, 1, 7) = 'sha256:'
        AND substr(submission_request_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    completed_at TEXT NOT NULL CHECK (julianday(completed_at) IS NOT NULL),
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(job_id),
    FOREIGN KEY (result_artifact_id) REFERENCES artifacts(artifact_id)
);

CREATE TABLE IF NOT EXISTS analysis_window_findings (
    analysis_id TEXT NOT NULL,
    ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
    window_id TEXT NOT NULL CHECK (
        length(window_id) = 71
        AND substr(window_id, 1, 7) = 'sha256:'
        AND substr(window_id, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    decision_state TEXT NOT NULL CHECK (
        decision_state IN (
            'normal', 'warning', 'rewrite_candidate',
            'topic_shift_only', 'low_confidence'
        )
    ),
    can_trigger_rewrite INTEGER NOT NULL CHECK (can_trigger_rewrite IN (0, 1)),
    payload_json TEXT NOT NULL CHECK (
        json_valid(payload_json) AND json_type(payload_json) = 'object'
    ),
    PRIMARY KEY (analysis_id, ordinal),
    UNIQUE (analysis_id, window_id),
    FOREIGN KEY (analysis_id) REFERENCES analysis_runs(analysis_id)
);

CREATE TABLE IF NOT EXISTS analysis_artifacts (
    artifact_id TEXT PRIMARY KEY,
    analysis_id TEXT NOT NULL UNIQUE,
    expires_at TEXT NOT NULL CHECK (julianday(expires_at) IS NOT NULL),
    uncompressed_bytes INTEGER NOT NULL CHECK (
        uncompressed_bytes BETWEEN 2 AND 16777216
    ),
    FOREIGN KEY (artifact_id) REFERENCES artifacts(artifact_id),
    FOREIGN KEY (analysis_id) REFERENCES analysis_runs(analysis_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX IF NOT EXISTS analysis_jobs_claim_idx
    ON analysis_jobs(status, lease_until, created_at, job_id);
CREATE INDEX IF NOT EXISTS analysis_jobs_novel_idx
    ON analysis_jobs(novel_id, created_at, job_id);
CREATE INDEX IF NOT EXISTS analysis_windows_page_idx
    ON analysis_window_findings(analysis_id, ordinal);
CREATE INDEX IF NOT EXISTS analysis_trace_content_idx
    ON artifacts(novel_id, kind, content_hash)
    WHERE kind = 'style-analysis-trace';

CREATE TRIGGER IF NOT EXISTS analysis_jobs_identity_immutable
BEFORE UPDATE ON analysis_jobs
WHEN NEW.job_id IS NOT OLD.job_id
  OR NEW.analysis_id IS NOT OLD.analysis_id
  OR NEW.novel_id IS NOT OLD.novel_id
  OR NEW.revision_id IS NOT OLD.revision_id
  OR NEW.revision_hash IS NOT OLD.revision_hash
  OR NEW.profile_id IS NOT OLD.profile_id
  OR NEW.profile_version_id IS NOT OLD.profile_version_id
  OR NEW.profile_version_hash IS NOT OLD.profile_version_hash
  OR NEW.analyzer_contract_hash IS NOT OLD.analyzer_contract_hash
  OR NEW.window_configuration_hash IS NOT OLD.window_configuration_hash
  OR NEW.snapshot_hash IS NOT OLD.snapshot_hash
  OR NEW.snapshot_json IS NOT OLD.snapshot_json
  OR NEW.max_attempts IS NOT OLD.max_attempts
  OR NEW.idempotency_key IS NOT OLD.idempotency_key
  OR NEW.request_hash IS NOT OLD.request_hash
  OR NEW.request_id IS NOT OLD.request_id
  OR NEW.actor_id IS NOT OLD.actor_id
  OR NEW.actor_key_id IS NOT OLD.actor_key_id
  OR NEW.retention_until IS NOT OLD.retention_until
  OR NEW.created_at IS NOT OLD.created_at
BEGIN
    SELECT RAISE(ABORT, 'analysis job snapshot and identity are immutable');
END;

CREATE TRIGGER IF NOT EXISTS analysis_jobs_attempt_monotonic
BEFORE UPDATE ON analysis_jobs
WHEN NEW.attempt < OLD.attempt
  OR julianday(NEW.updated_at) < julianday(OLD.updated_at)
BEGIN
    SELECT RAISE(ABORT, 'analysis job attempt and time are monotonic');
END;

CREATE TRIGGER IF NOT EXISTS analysis_jobs_transition_guard
BEFORE UPDATE ON analysis_jobs
WHEN NOT (
    (OLD.status = 'queued' AND NEW.status = 'running' AND NEW.attempt = 1)
    OR
    (OLD.status = 'running' AND NEW.status = 'running'
        AND NEW.attempt = OLD.attempt + 1)
    OR
    (OLD.status = 'running' AND NEW.status IN ('succeeded', 'failed')
        AND NEW.attempt = OLD.attempt)
)
BEGIN
    SELECT RAISE(ABORT, 'analysis job transition is invalid or terminal');
END;

CREATE TRIGGER IF NOT EXISTS analysis_jobs_no_delete
BEFORE DELETE ON analysis_jobs BEGIN
    SELECT RAISE(ABORT, 'analysis jobs are retained');
END;
CREATE TRIGGER IF NOT EXISTS analysis_claim_receipts_no_update
BEFORE UPDATE ON analysis_claim_receipts BEGIN
    SELECT RAISE(ABORT, 'analysis claim receipts are immutable');
END;
CREATE TRIGGER IF NOT EXISTS analysis_claim_receipts_no_delete
BEFORE DELETE ON analysis_claim_receipts BEGIN
    SELECT RAISE(ABORT, 'analysis claim receipts are immutable');
END;
CREATE TRIGGER IF NOT EXISTS analysis_runs_no_update
BEFORE UPDATE ON analysis_runs BEGIN
    SELECT RAISE(ABORT, 'analysis results are immutable');
END;
CREATE TRIGGER IF NOT EXISTS analysis_runs_no_delete
BEFORE DELETE ON analysis_runs BEGIN
    SELECT RAISE(ABORT, 'analysis results are immutable');
END;
CREATE TRIGGER IF NOT EXISTS analysis_windows_no_update
BEFORE UPDATE ON analysis_window_findings BEGIN
    SELECT RAISE(ABORT, 'analysis window findings are immutable');
END;
CREATE TRIGGER IF NOT EXISTS analysis_windows_no_delete
BEFORE DELETE ON analysis_window_findings BEGIN
    SELECT RAISE(ABORT, 'analysis window findings are immutable');
END;
CREATE TRIGGER IF NOT EXISTS analysis_artifacts_no_update
BEFORE UPDATE ON analysis_artifacts BEGIN
    SELECT RAISE(ABORT, 'analysis artifact metadata is immutable');
END;
CREATE TRIGGER IF NOT EXISTS analysis_artifacts_no_delete
BEFORE DELETE ON analysis_artifacts BEGIN
    SELECT RAISE(ABORT, 'analysis artifact metadata is immutable');
END;

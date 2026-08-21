CREATE TABLE IF NOT EXISTS import_receipts (
    idempotency_key TEXT PRIMARY KEY,
    request_hash TEXT NOT NULL CHECK (request_hash GLOB 'sha256:*'),
    novel_id TEXT NOT NULL,
    imported_at TEXT NOT NULL,
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id)
);

CREATE TABLE IF NOT EXISTS artifacts (
    artifact_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    kind TEXT NOT NULL,
    media_type TEXT NOT NULL,
    codec TEXT NOT NULL,
    content_hash TEXT NOT NULL CHECK (content_hash GLOB 'sha256:*'),
    size_bytes INTEGER NOT NULL CHECK (size_bytes >= 0),
    content BLOB NOT NULL,
    created_at TEXT NOT NULL,
    portable INTEGER NOT NULL CHECK (portable IN (0, 1)),
    CHECK (length(content) = size_bytes),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id)
);

CREATE TABLE IF NOT EXISTS export_jobs (
    job_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    revision_sequence INTEGER NOT NULL CHECK (revision_sequence >= 0),
    revision_hash TEXT NOT NULL CHECK (revision_hash GLOB 'sha256:*'),
    format TEXT NOT NULL CHECK (format IN ('canonical-revision', 'canonical-package')),
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL CHECK (request_hash GLOB 'sha256:*'),
    result_artifact_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (novel_id, idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id),
    FOREIGN KEY (result_artifact_id) REFERENCES artifacts(artifact_id)
);

CREATE INDEX IF NOT EXISTS artifacts_novel_revision_idx
    ON artifacts(novel_id, revision_id, portable);
CREATE INDEX IF NOT EXISTS export_jobs_novel_created_idx
    ON export_jobs(novel_id, created_at);

CREATE TRIGGER IF NOT EXISTS import_receipts_no_update
BEFORE UPDATE ON import_receipts BEGIN
    SELECT RAISE(ABORT, 'import receipts are immutable');
END;
CREATE TRIGGER IF NOT EXISTS import_receipts_no_delete
BEFORE DELETE ON import_receipts BEGIN
    SELECT RAISE(ABORT, 'import receipts are immutable');
END;
CREATE TRIGGER IF NOT EXISTS artifacts_no_update
BEFORE UPDATE ON artifacts BEGIN
    SELECT RAISE(ABORT, 'artifacts are immutable');
END;
CREATE TRIGGER IF NOT EXISTS export_jobs_no_update
BEFORE UPDATE ON export_jobs BEGIN
    SELECT RAISE(ABORT, 'completed export jobs are immutable');
END;

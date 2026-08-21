CREATE TABLE IF NOT EXISTS access_keys (
    key_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    secret_digest BLOB NOT NULL CHECK (length(secret_digest) = 32),
    scopes_json TEXT NOT NULL
        CHECK (json_valid(scopes_json)
               AND json_type(scopes_json) = 'array'
               AND json_array_length(scopes_json) > 0),
    actor_id TEXT NOT NULL CHECK (length(actor_id) BETWEEN 1 AND 128),
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    revoked_at TEXT,
    last_used_at TEXT,
    issue_idempotency_key TEXT NOT NULL,
    issue_request_hash TEXT NOT NULL CHECK (
        length(issue_request_hash) = 71
        AND substr(issue_request_hash, 1, 7) = 'sha256:'
        AND substr(issue_request_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    UNIQUE (novel_id, issue_idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id)
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id TEXT PRIMARY KEY,
    occurred_at TEXT NOT NULL,
    request_id TEXT NOT NULL CHECK (length(request_id) BETWEEN 1 AND 128),
    actor_id TEXT NOT NULL CHECK (length(actor_id) BETWEEN 1 AND 128),
    actor_key_id TEXT,
    novel_id TEXT NOT NULL,
    action TEXT NOT NULL CHECK (action IN (
        'access-key-issue', 'access-key-revoke', 'canonical-import',
        'canonical-export', 'commit'
    )),
    subject_id TEXT CHECK (subject_id IS NULL OR length(subject_id) BETWEEN 1 AND 128),
    operation_id TEXT,
    revision_id TEXT,
    result TEXT NOT NULL CHECK (result IN ('succeeded', 'idempotent', 'rejected')),
    operation_hash TEXT CHECK (
        operation_hash IS NULL OR (
            length(operation_hash) = 71
            AND substr(operation_hash, 1, 7) = 'sha256:'
            AND substr(operation_hash, 8) NOT GLOB '*[^0-9a-f]*'
        )
    ),
    content_hash TEXT CHECK (
        content_hash IS NULL OR (
            length(content_hash) = 71
            AND substr(content_hash, 1, 7) = 'sha256:'
            AND substr(content_hash, 8) NOT GLOB '*[^0-9a-f]*'
        )
    ),
    event_hash TEXT NOT NULL CHECK (
        length(event_hash) = 71
        AND substr(event_hash, 1, 7) = 'sha256:'
        AND substr(event_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    FOREIGN KEY (actor_key_id) REFERENCES access_keys(key_id),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (operation_id) REFERENCES operations(operation_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id)
);

CREATE INDEX IF NOT EXISTS access_keys_novel_idx
    ON access_keys(novel_id, revoked_at, expires_at);
CREATE INDEX IF NOT EXISTS audit_events_novel_time_idx
    ON audit_events(novel_id, occurred_at, event_id);
CREATE INDEX IF NOT EXISTS audit_events_request_idx
    ON audit_events(request_id);

CREATE TRIGGER IF NOT EXISTS access_keys_identity_immutable
BEFORE UPDATE ON access_keys
WHEN NEW.key_id IS NOT OLD.key_id
  OR NEW.novel_id IS NOT OLD.novel_id
  OR NEW.secret_digest IS NOT OLD.secret_digest
  OR NEW.scopes_json IS NOT OLD.scopes_json
  OR NEW.actor_id IS NOT OLD.actor_id
  OR NEW.created_at IS NOT OLD.created_at
  OR NEW.expires_at IS NOT OLD.expires_at
  OR NEW.issue_idempotency_key IS NOT OLD.issue_idempotency_key
  OR NEW.issue_request_hash IS NOT OLD.issue_request_hash
BEGIN
    SELECT RAISE(ABORT, 'access key identity is immutable');
END;

CREATE TRIGGER IF NOT EXISTS access_keys_revocation_monotonic
BEFORE UPDATE ON access_keys
WHEN OLD.revoked_at IS NOT NULL AND NEW.revoked_at IS NOT OLD.revoked_at
BEGIN
    SELECT RAISE(ABORT, 'access key revocation is permanent');
END;

CREATE TRIGGER IF NOT EXISTS access_keys_last_used_monotonic
BEFORE UPDATE ON access_keys
WHEN OLD.last_used_at IS NOT NULL
 AND (NEW.last_used_at IS NULL OR NEW.last_used_at < OLD.last_used_at)
BEGIN
    SELECT RAISE(ABORT, 'access key last use is monotonic');
END;

CREATE TRIGGER IF NOT EXISTS access_keys_no_delete
BEFORE DELETE ON access_keys BEGIN
    SELECT RAISE(ABORT, 'access keys are retained for audit');
END;

CREATE TRIGGER IF NOT EXISTS audit_events_no_update
BEFORE UPDATE ON audit_events BEGIN
    SELECT RAISE(ABORT, 'audit events are append-only');
END;

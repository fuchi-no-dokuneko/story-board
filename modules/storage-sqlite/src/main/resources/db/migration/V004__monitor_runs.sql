CREATE TABLE IF NOT EXISTS monitor_runs (
    run_id TEXT PRIMARY KEY,
    output_id TEXT NOT NULL UNIQUE,
    output_kind TEXT NOT NULL CHECK (output_kind IN ('finding', 'proposed_operation')),
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    revision_hash TEXT NOT NULL CHECK (
        length(revision_hash) = 71
        AND substr(revision_hash, 1, 7) = 'sha256:'
        AND substr(revision_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    target_block_id TEXT NOT NULL,
    neighbor_count INTEGER NOT NULL CHECK (neighbor_count IN (1, 2)),
    monitor_version TEXT NOT NULL CHECK (length(monitor_version) BETWEEN 1 AND 128),
    rule_version TEXT NOT NULL CHECK (length(rule_version) BETWEEN 1 AND 128),
    affected_blocks_json TEXT NOT NULL CHECK (
        json_valid(affected_blocks_json)
        AND json_type(affected_blocks_json) = 'array'
        AND json_array_length(affected_blocks_json) BETWEEN 1 AND 5
    ),
    idempotency_key TEXT NOT NULL CHECK (length(idempotency_key) BETWEEN 1 AND 200),
    request_hash TEXT NOT NULL CHECK (
        length(request_hash) = 71
        AND substr(request_hash, 1, 7) = 'sha256:'
        AND substr(request_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    request_id TEXT NOT NULL CHECK (length(request_id) BETWEEN 1 AND 128),
    actor_id TEXT NOT NULL CHECK (length(actor_id) BETWEEN 1 AND 128),
    actor_key_id TEXT,
    submitted_at TEXT NOT NULL,
    UNIQUE (novel_id, idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id),
    FOREIGN KEY (actor_key_id) REFERENCES access_keys(key_id)
);

CREATE TABLE IF NOT EXISTS monitor_issues (
    issue_id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL UNIQUE,
    payload_json TEXT NOT NULL CHECK (
        json_valid(payload_json)
        AND json_extract(payload_json, '$.kind') = 'finding'
    ),
    FOREIGN KEY (run_id) REFERENCES monitor_runs(run_id),
    FOREIGN KEY (issue_id) REFERENCES monitor_runs(output_id)
);

CREATE TABLE IF NOT EXISTS monitor_proposed_operations (
    proposal_id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL UNIQUE,
    payload_json TEXT NOT NULL CHECK (
        json_valid(payload_json)
        AND json_extract(payload_json, '$.kind') = 'proposed_operation'
    ),
    FOREIGN KEY (run_id) REFERENCES monitor_runs(run_id),
    FOREIGN KEY (proposal_id) REFERENCES monitor_runs(output_id)
);

CREATE INDEX IF NOT EXISTS monitor_runs_novel_revision_idx
    ON monitor_runs(novel_id, revision_id, submitted_at, run_id);
CREATE INDEX IF NOT EXISTS monitor_runs_target_idx
    ON monitor_runs(novel_id, target_block_id, submitted_at, run_id);

CREATE TRIGGER IF NOT EXISTS monitor_runs_revision_novel_match
BEFORE INSERT ON monitor_runs
WHEN NOT EXISTS (
    SELECT 1 FROM revisions
    WHERE revision_id = NEW.revision_id AND novel_id = NEW.novel_id
)
BEGIN
    SELECT RAISE(ABORT, 'monitor revision does not belong to novel');
END;

CREATE TRIGGER IF NOT EXISTS monitor_runs_no_update
BEFORE UPDATE ON monitor_runs BEGIN
    SELECT RAISE(ABORT, 'monitor runs are append-only');
END;
CREATE TRIGGER IF NOT EXISTS monitor_runs_no_delete
BEFORE DELETE ON monitor_runs BEGIN
    SELECT RAISE(ABORT, 'monitor runs are append-only');
END;
CREATE TRIGGER IF NOT EXISTS monitor_issues_no_update
BEFORE UPDATE ON monitor_issues BEGIN
    SELECT RAISE(ABORT, 'monitor issues are append-only');
END;
CREATE TRIGGER IF NOT EXISTS monitor_issues_no_delete
BEFORE DELETE ON monitor_issues BEGIN
    SELECT RAISE(ABORT, 'monitor issues are append-only');
END;
CREATE TRIGGER IF NOT EXISTS monitor_proposals_no_update
BEFORE UPDATE ON monitor_proposed_operations BEGIN
    SELECT RAISE(ABORT, 'monitor proposals are append-only');
END;
CREATE TRIGGER IF NOT EXISTS monitor_proposals_no_delete
BEFORE DELETE ON monitor_proposed_operations BEGIN
    SELECT RAISE(ABORT, 'monitor proposals are append-only');
END;

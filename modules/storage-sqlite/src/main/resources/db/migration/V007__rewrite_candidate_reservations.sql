CREATE TABLE IF NOT EXISTS rewrite_candidate_reservations (
    proposal_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    analysis_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    revision_hash TEXT NOT NULL,
    profile_version_id TEXT NOT NULL,
    eligibility_hash TEXT NOT NULL,
    worker_input_hash TEXT NOT NULL,
    reservation_hash TEXT NOT NULL UNIQUE,
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    reservation_json TEXT NOT NULL CHECK (json_valid(reservation_json)),
    created_at TEXT NOT NULL,
    cooldown_until TEXT NOT NULL,
    UNIQUE (novel_id, idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (analysis_id) REFERENCES analysis_jobs(analysis_id)
);

CREATE TABLE IF NOT EXISTS rewrite_reserved_findings (
    analysis_id TEXT NOT NULL,
    finding_id TEXT NOT NULL,
    proposal_id TEXT NOT NULL,
    PRIMARY KEY (analysis_id, finding_id),
    FOREIGN KEY (proposal_id) REFERENCES rewrite_candidate_reservations(proposal_id)
);

CREATE TABLE IF NOT EXISTS rewrite_reserved_blocks (
    proposal_id TEXT NOT NULL,
    novel_id TEXT NOT NULL,
    block_id TEXT NOT NULL,
    PRIMARY KEY (proposal_id, block_id),
    FOREIGN KEY (proposal_id) REFERENCES rewrite_candidate_reservations(proposal_id),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id)
);

CREATE INDEX IF NOT EXISTS rewrite_reservations_cooldown
    ON rewrite_candidate_reservations(novel_id, cooldown_until);
CREATE INDEX IF NOT EXISTS rewrite_blocks_overlap
    ON rewrite_reserved_blocks(novel_id, block_id);

CREATE TRIGGER IF NOT EXISTS rewrite_reservations_no_update
BEFORE UPDATE ON rewrite_candidate_reservations BEGIN
    SELECT RAISE(ABORT, 'rewrite candidate reservations are append-only');
END;
CREATE TRIGGER IF NOT EXISTS rewrite_reservations_no_delete
BEFORE DELETE ON rewrite_candidate_reservations BEGIN
    SELECT RAISE(ABORT, 'rewrite candidate reservations are append-only');
END;
CREATE TRIGGER IF NOT EXISTS rewrite_findings_no_update
BEFORE UPDATE ON rewrite_reserved_findings BEGIN
    SELECT RAISE(ABORT, 'rewrite finding reservations are append-only');
END;
CREATE TRIGGER IF NOT EXISTS rewrite_findings_no_delete
BEFORE DELETE ON rewrite_reserved_findings BEGIN
    SELECT RAISE(ABORT, 'rewrite finding reservations are append-only');
END;
CREATE TRIGGER IF NOT EXISTS rewrite_blocks_no_update
BEFORE UPDATE ON rewrite_reserved_blocks BEGIN
    SELECT RAISE(ABORT, 'rewrite block reservations are append-only');
END;
CREATE TRIGGER IF NOT EXISTS rewrite_blocks_no_delete
BEFORE DELETE ON rewrite_reserved_blocks BEGIN
    SELECT RAISE(ABORT, 'rewrite block reservations are append-only');
END;

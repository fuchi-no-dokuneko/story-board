CREATE TABLE IF NOT EXISTS novels (
    novel_id TEXT PRIMARY KEY,
    head_revision_id TEXT NOT NULL,
    head_sequence INTEGER NOT NULL CHECK (head_sequence >= 0),
    head_hash TEXT NOT NULL CHECK (head_hash GLOB 'sha256:*'),
    schema_version TEXT NOT NULL,
    FOREIGN KEY (head_revision_id) REFERENCES revisions(revision_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE IF NOT EXISTS revisions (
    revision_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    parent_revision_id TEXT,
    sequence INTEGER NOT NULL CHECK (sequence >= 0),
    content_hash TEXT NOT NULL CHECK (content_hash GLOB 'sha256:*'),
    canonical_json BLOB NOT NULL CHECK (length(canonical_json) > 0),
    created_at TEXT NOT NULL,
    operation_id TEXT,
    UNIQUE (novel_id, sequence),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (parent_revision_id) REFERENCES revisions(revision_id),
    FOREIGN KEY (operation_id) REFERENCES operations(operation_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE IF NOT EXISTS operations (
    operation_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    sequence INTEGER NOT NULL CHECK (sequence > 0),
    base_revision_id TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    operation_hash TEXT NOT NULL CHECK (operation_hash GLOB 'sha256:*'),
    idempotency_key TEXT NOT NULL,
    payload_json TEXT NOT NULL CHECK (json_valid(payload_json)),
    result_revision_id TEXT NOT NULL,
    result_hash TEXT NOT NULL CHECK (result_hash GLOB 'sha256:*'),
    committed_at TEXT NOT NULL,
    UNIQUE (novel_id, sequence),
    UNIQUE (novel_id, idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (base_revision_id) REFERENCES revisions(revision_id),
    FOREIGN KEY (result_revision_id) REFERENCES revisions(revision_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE IF NOT EXISTS checkpoints (
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    sequence INTEGER NOT NULL CHECK (sequence >= 0),
    content_hash TEXT NOT NULL CHECK (content_hash GLOB 'sha256:*'),
    codec TEXT NOT NULL,
    uncompressed_bytes INTEGER NOT NULL CHECK (uncompressed_bytes > 0),
    compressed_json BLOB NOT NULL CHECK (length(compressed_json) > 0),
    created_at TEXT NOT NULL,
    PRIMARY KEY (novel_id, sequence),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id)
);

CREATE TABLE IF NOT EXISTS block_tombstones (
    novel_id TEXT NOT NULL,
    operation_id TEXT NOT NULL,
    deleted_in_revision_id TEXT NOT NULL,
    source_scene_id TEXT NOT NULL,
    block_id TEXT NOT NULL,
    block_version_id TEXT NOT NULL,
    block_json TEXT NOT NULL CHECK (json_valid(block_json)),
    PRIMARY KEY (operation_id, block_id),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (operation_id) REFERENCES operations(operation_id),
    FOREIGN KEY (deleted_in_revision_id) REFERENCES revisions(revision_id)
);

CREATE TABLE IF NOT EXISTS head_block_projection (
    novel_id TEXT NOT NULL,
    chapter_id TEXT NOT NULL,
    scene_id TEXT NOT NULL,
    block_id TEXT NOT NULL,
    block_version_id TEXT NOT NULL,
    order_key TEXT NOT NULL,
    text_hash TEXT NOT NULL CHECK (text_hash GLOB 'sha256:*'),
    PRIMARY KEY (novel_id, block_id),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id)
);

CREATE INDEX IF NOT EXISTS revisions_novel_parent_idx
    ON revisions(novel_id, parent_revision_id);
CREATE INDEX IF NOT EXISTS operations_novel_base_idx
    ON operations(novel_id, base_revision_id);
CREATE INDEX IF NOT EXISTS projection_scene_order_idx
    ON head_block_projection(novel_id, scene_id, order_key);

CREATE TRIGGER IF NOT EXISTS revisions_no_update
BEFORE UPDATE ON revisions BEGIN
    SELECT RAISE(ABORT, 'revisions are append-only');
END;
CREATE TRIGGER IF NOT EXISTS revisions_no_delete
BEFORE DELETE ON revisions BEGIN
    SELECT RAISE(ABORT, 'revisions are append-only');
END;
CREATE TRIGGER IF NOT EXISTS operations_no_update
BEFORE UPDATE ON operations BEGIN
    SELECT RAISE(ABORT, 'operations are append-only');
END;
CREATE TRIGGER IF NOT EXISTS operations_no_delete
BEFORE DELETE ON operations BEGIN
    SELECT RAISE(ABORT, 'operations are append-only');
END;
CREATE TRIGGER IF NOT EXISTS checkpoints_no_update
BEFORE UPDATE ON checkpoints BEGIN
    SELECT RAISE(ABORT, 'checkpoints are append-only');
END;
CREATE TRIGGER IF NOT EXISTS checkpoints_no_delete
BEFORE DELETE ON checkpoints BEGIN
    SELECT RAISE(ABORT, 'checkpoints are append-only');
END;
CREATE TRIGGER IF NOT EXISTS tombstones_no_update
BEFORE UPDATE ON block_tombstones BEGIN
    SELECT RAISE(ABORT, 'tombstones are append-only');
END;
CREATE TRIGGER IF NOT EXISTS tombstones_no_delete
BEFORE DELETE ON block_tombstones BEGIN
    SELECT RAISE(ABORT, 'tombstones are append-only');
END;

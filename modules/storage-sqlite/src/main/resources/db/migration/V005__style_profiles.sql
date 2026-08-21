CREATE TABLE IF NOT EXISTS style_profiles (
    profile_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    profile_json TEXT NOT NULL CHECK (
        json_valid(profile_json)
        AND json_type(profile_json) = 'object'
    ),
    resource_hash TEXT NOT NULL UNIQUE CHECK (
        length(resource_hash) = 71
        AND substr(resource_hash, 1, 7) = 'sha256:'
        AND substr(resource_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    created_by TEXT NOT NULL CHECK (length(created_by) BETWEEN 1 AND 128),
    created_at TEXT NOT NULL,
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id)
);

CREATE TABLE IF NOT EXISTS style_profile_versions (
    version_id TEXT PRIMARY KEY,
    profile_id TEXT NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    version_json TEXT NOT NULL CHECK (
        json_valid(version_json)
        AND json_type(version_json) = 'object'
    ),
    version_hash TEXT NOT NULL UNIQUE CHECK (
        length(version_hash) = 71
        AND substr(version_hash, 1, 7) = 'sha256:'
        AND substr(version_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    created_by TEXT NOT NULL CHECK (length(created_by) BETWEEN 1 AND 128),
    created_at TEXT NOT NULL,
    UNIQUE (profile_id, version),
    UNIQUE (profile_id, version_id),
    FOREIGN KEY (profile_id) REFERENCES style_profiles(profile_id)
);

CREATE TABLE IF NOT EXISTS style_profile_lifecycle_events (
    event_id TEXT PRIMARY KEY,
    profile_id TEXT NOT NULL,
    version_id TEXT NOT NULL,
    sequence INTEGER NOT NULL CHECK (sequence > 0),
    from_state TEXT,
    to_state TEXT NOT NULL CHECK (
        to_state IN ('draft', 'calibrating', 'ready', 'deprecated')
    ),
    event_json TEXT NOT NULL CHECK (
        json_valid(event_json)
        AND json_type(event_json) = 'object'
    ),
    request_id TEXT NOT NULL CHECK (length(request_id) BETWEEN 1 AND 128),
    actor_id TEXT NOT NULL CHECK (length(actor_id) BETWEEN 1 AND 128),
    actor_key_id TEXT,
    occurred_at TEXT NOT NULL,
    UNIQUE (version_id, sequence),
    FOREIGN KEY (profile_id, version_id)
        REFERENCES style_profile_versions(profile_id, version_id),
    FOREIGN KEY (actor_key_id) REFERENCES access_keys(key_id),
    CHECK (
        (sequence = 1 AND from_state IS NULL AND to_state = 'draft')
        OR
        (sequence > 1 AND from_state IS NOT NULL)
    ),
    CHECK (
        from_state IS NULL
        OR from_state IN ('draft', 'calibrating', 'ready', 'deprecated')
    )
);

CREATE TABLE IF NOT EXISTS style_profile_mutations (
    novel_id TEXT NOT NULL,
    idempotency_key TEXT NOT NULL CHECK (
        length(idempotency_key) BETWEEN 1 AND 200
    ),
    mutation_kind TEXT NOT NULL CHECK (
        mutation_kind IN ('create_profile', 'create_version', 'transition')
    ),
    request_hash TEXT NOT NULL CHECK (
        length(request_hash) = 71
        AND substr(request_hash, 1, 7) = 'sha256:'
        AND substr(request_hash, 8) NOT GLOB '*[^0-9a-f]*'
    ),
    profile_id TEXT NOT NULL,
    version_id TEXT,
    event_id TEXT,
    request_id TEXT NOT NULL CHECK (length(request_id) BETWEEN 1 AND 128),
    actor_id TEXT NOT NULL CHECK (length(actor_id) BETWEEN 1 AND 128),
    actor_key_id TEXT,
    created_at TEXT NOT NULL,
    PRIMARY KEY (novel_id, idempotency_key),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (profile_id) REFERENCES style_profiles(profile_id),
    FOREIGN KEY (version_id) REFERENCES style_profile_versions(version_id),
    FOREIGN KEY (event_id) REFERENCES style_profile_lifecycle_events(event_id),
    FOREIGN KEY (actor_key_id) REFERENCES access_keys(key_id),
    CHECK (
        (mutation_kind = 'create_profile' AND version_id IS NULL AND event_id IS NULL)
        OR
        (mutation_kind = 'create_version' AND version_id IS NOT NULL AND event_id IS NOT NULL)
        OR
        (mutation_kind = 'transition' AND version_id IS NOT NULL AND event_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS style_profiles_novel_idx
    ON style_profiles(novel_id, profile_id);
CREATE INDEX IF NOT EXISTS style_versions_profile_idx
    ON style_profile_versions(profile_id, version, version_id);
CREATE INDEX IF NOT EXISTS style_lifecycle_current_idx
    ON style_profile_lifecycle_events(profile_id, version_id, sequence DESC);

CREATE TRIGGER IF NOT EXISTS style_profiles_no_update
BEFORE UPDATE ON style_profiles BEGIN
    SELECT RAISE(ABORT, 'style profiles are immutable');
END;
CREATE TRIGGER IF NOT EXISTS style_profiles_no_delete
BEFORE DELETE ON style_profiles BEGIN
    SELECT RAISE(ABORT, 'style profiles are immutable');
END;
CREATE TRIGGER IF NOT EXISTS style_versions_no_update
BEFORE UPDATE ON style_profile_versions BEGIN
    SELECT RAISE(ABORT, 'style profile versions are immutable');
END;
CREATE TRIGGER IF NOT EXISTS style_versions_no_delete
BEFORE DELETE ON style_profile_versions BEGIN
    SELECT RAISE(ABORT, 'style profile versions are immutable');
END;
CREATE TRIGGER IF NOT EXISTS style_lifecycle_no_update
BEFORE UPDATE ON style_profile_lifecycle_events BEGIN
    SELECT RAISE(ABORT, 'style lifecycle events are append-only');
END;
CREATE TRIGGER IF NOT EXISTS style_lifecycle_no_delete
BEFORE DELETE ON style_profile_lifecycle_events BEGIN
    SELECT RAISE(ABORT, 'style lifecycle events are append-only');
END;
CREATE TRIGGER IF NOT EXISTS style_mutations_no_update
BEFORE UPDATE ON style_profile_mutations BEGIN
    SELECT RAISE(ABORT, 'style mutation records are append-only');
END;
CREATE TRIGGER IF NOT EXISTS style_mutations_no_delete
BEFORE DELETE ON style_profile_mutations BEGIN
    SELECT RAISE(ABORT, 'style mutation records are append-only');
END;

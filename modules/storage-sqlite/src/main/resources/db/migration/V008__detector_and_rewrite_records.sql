CREATE TABLE detector_runs (
    detector_run_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    revision_hash TEXT NOT NULL CHECK (revision_hash GLOB 'sha256:*'),
    detector_version TEXT NOT NULL,
    request_hash TEXT NOT NULL CHECK (request_hash GLOB 'sha256:*'),
    created_at TEXT NOT NULL,
    UNIQUE (novel_id, request_hash),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id)
);

CREATE TABLE issues (
    issue_id TEXT PRIMARY KEY,
    detector_run_id TEXT NOT NULL,
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    code TEXT NOT NULL,
    severity TEXT NOT NULL CHECK (severity IN ('info', 'warning', 'error')),
    evidence_json TEXT NOT NULL CHECK (json_valid(evidence_json)),
    created_at TEXT NOT NULL,
    FOREIGN KEY (detector_run_id) REFERENCES detector_runs(detector_run_id),
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id)
);

CREATE TABLE rewrite_proposals (
    proposal_id TEXT PRIMARY KEY,
    novel_id TEXT NOT NULL,
    revision_id TEXT NOT NULL,
    revision_hash TEXT NOT NULL CHECK (revision_hash GLOB 'sha256:*'),
    profile_version_id TEXT NOT NULL,
    candidate_hash TEXT NOT NULL CHECK (candidate_hash GLOB 'sha256:*'),
    proposal_json TEXT NOT NULL CHECK (json_valid(proposal_json)),
    state TEXT NOT NULL CHECK (state IN ('proposed', 'stale', 'accepted', 'rejected')),
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    FOREIGN KEY (novel_id) REFERENCES novels(novel_id),
    FOREIGN KEY (revision_id) REFERENCES revisions(revision_id),
    FOREIGN KEY (profile_version_id) REFERENCES style_profile_versions(version_id)
);

CREATE INDEX detector_runs_revision_idx
    ON detector_runs(novel_id, revision_id, created_at);
CREATE INDEX issues_run_idx ON issues(detector_run_id, severity, code);
CREATE INDEX rewrite_proposals_revision_idx
    ON rewrite_proposals(novel_id, revision_id, state, created_at);

CREATE TRIGGER detector_runs_no_update
BEFORE UPDATE ON detector_runs BEGIN
    SELECT RAISE(ABORT, 'detector runs are append-only');
END;
CREATE TRIGGER detector_runs_no_delete
BEFORE DELETE ON detector_runs BEGIN
    SELECT RAISE(ABORT, 'detector runs are append-only');
END;
CREATE TRIGGER issues_no_update
BEFORE UPDATE ON issues BEGIN
    SELECT RAISE(ABORT, 'issues are append-only');
END;
CREATE TRIGGER issues_no_delete
BEFORE DELETE ON issues BEGIN
    SELECT RAISE(ABORT, 'issues are append-only');
END;
CREATE TRIGGER rewrite_proposals_identity_immutable
BEFORE UPDATE ON rewrite_proposals
WHEN NEW.proposal_id <> OLD.proposal_id
  OR NEW.novel_id <> OLD.novel_id
  OR NEW.revision_id <> OLD.revision_id
  OR NEW.revision_hash <> OLD.revision_hash
  OR NEW.profile_version_id <> OLD.profile_version_id
  OR NEW.candidate_hash <> OLD.candidate_hash
  OR NEW.proposal_json <> OLD.proposal_json
  OR NEW.created_at <> OLD.created_at
  OR NEW.expires_at <> OLD.expires_at
BEGIN
    SELECT RAISE(ABORT, 'rewrite proposal identity is immutable');
END;
CREATE TRIGGER rewrite_proposals_state_guard
BEFORE UPDATE OF state ON rewrite_proposals
WHEN NOT (
    OLD.state = 'proposed'
    AND NEW.state IN ('stale', 'accepted', 'rejected')
)
BEGIN
    SELECT RAISE(ABORT, 'invalid rewrite proposal state transition');
END;
CREATE TRIGGER rewrite_proposals_no_delete
BEFORE DELETE ON rewrite_proposals BEGIN
    SELECT RAISE(ABORT, 'rewrite proposals cannot be deleted');
END;

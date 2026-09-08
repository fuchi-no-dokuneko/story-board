package dev.storyblock.storage.sqlite;

import dev.storyblock.style.StyleAnalysisJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteAnalysisInsertJob {
    static void insert(Connection connection, StyleAnalysisJob job) throws SQLException {
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
            SqliteAnalysisBindJob.bind(statement, job);
            statement.executeUpdate();
        }

    }
}

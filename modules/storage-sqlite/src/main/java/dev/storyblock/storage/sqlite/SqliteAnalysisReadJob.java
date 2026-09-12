package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobStatus;
import dev.storyblock.style.StyleAnalysisSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteAnalysisReadJob {
    static StyleAnalysisJob readJob(ResultSet result) throws SQLException {
        StyleAnalysisSnapshot snapshot = StyleAnalysisSnapshot.fromCanonical(SqliteAnalysisParseObject.parseObject(
                result.getString("snapshot_json"), "style analysis snapshot"
        ));
        String actorKey = result.getString("actor_key_id");
        AuditContext audit = new AuditContext(
                result.getString("request_id"),
                result.getString("actor_id"),
                actorKey == null ? null : new Ids.AccessKeyId(actorKey),
                Instant.parse(result.getString("created_at"))
        );
        StyleAnalysisJob job = new StyleAnalysisJob(
                new Ids.JobId(result.getString("job_id")),
                new Ids.StyleAnalysisId(result.getString("analysis_id")),
                snapshot,
                StyleAnalysisJobStatus.fromCanonicalName(result.getString("status")),
                result.getString("lease_owner"),
                SqliteAnalysisNullableInstant.nullableInstant(result.getString("lease_until")),
                result.getInt("attempt"),
                result.getInt("max_attempts"),
                result.getString("idempotency_key"),
                result.getString("request_hash"),
                SqliteAnalysisNullableArtifact.nullableArtifact(result.getString("result_artifact_id")),
                result.getString("result_hash"),
                result.getString("failure_code"),
                audit,
                Instant.parse(result.getString("retention_until")),
                audit.occurredAt(),
                Instant.parse(result.getString("updated_at"))
        );
        SqliteAnalysisVerifySnapshot.verify(result, snapshot);
        return job;
    }
}

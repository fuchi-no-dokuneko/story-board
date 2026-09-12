package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StoredExportJob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteTransferReadExportJob {
    static StoredExportJob readExportJob(ResultSet result) throws SQLException {
        return new StoredExportJob(
                new Ids.JobId(result.getString("job_id")),
                new Ids.NovelId(result.getString("novel_id")),
                new RevisionRef(
                        new Ids.RevisionId(result.getString("revision_id")),
                        result.getLong("revision_sequence"),
                        result.getString("revision_hash")
                ),
                CanonicalExportFormat.fromCanonicalName(result.getString("format")),
                new Ids.ArtifactId(result.getString("result_artifact_id")),
                Instant.parse(result.getString("created_at"))
        );
    }
}

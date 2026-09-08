package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import dev.storyblock.style.StyleAnalysisSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;

final class SqliteAnalysisVerifySnapshot {
    static void verify(ResultSet result, StyleAnalysisSnapshot snapshot) throws SQLException {
        if (!snapshot.novelId().value().equals(result.getString("novel_id"))
                || !snapshot.revisionId().value().equals(result.getString("revision_id"))
                || !snapshot.revisionHash().equals(result.getString("revision_hash"))
                || !snapshot.profileVersion().profileId().value().equals(
                        result.getString("profile_id")
                )
                || !snapshot.profileVersion().versionId().value().equals(
                        result.getString("profile_version_id")
                )
                || !snapshot.profileVersionHash().equals(
                        result.getString("profile_version_hash")
                )
                || !snapshot.analyzerContractHash().equals(
                        result.getString("analyzer_contract_hash")
                )
                || !snapshot.windowConfigurationHash().equals(
                        result.getString("window_configuration_hash")
                )
                || !snapshot.snapshotHash().equals(result.getString("snapshot_hash"))) {
            throw new StorageException("Stored style analysis snapshot integrity check failed");
        }

    }
}

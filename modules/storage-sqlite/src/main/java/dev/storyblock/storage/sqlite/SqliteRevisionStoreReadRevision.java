package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.StorageException;
import dev.storyblock.storage.StoredRevision;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

final class SqliteRevisionStoreReadRevision {
    static StoredRevision readRevision(
            ResultSet result,
            Ids.NovelId novelId,
            Ids.RevisionId revisionId
    ) throws SQLException {
        String storedHash = result.getString("content_hash");
        CanonicalRevision canonical = CanonicalRevision.parseEnvelope(result.getBytes("canonical_json"));
        if (!canonical.contentHash().equals(storedHash)) {
            throw new StorageException("Stored revision hash does not match " + revisionId.value());
        }
        RevisionManifest manifest = NarrativeCanonicalMapper.fromCanonical(canonical);
        if (!manifest.novel().id().equals(novelId) || !manifest.id().equals(revisionId)) {
            throw new StorageException("Stored revision identity does not match relational columns");
        }
        String relationalParent = result.getString("parent_revision_id");
        String canonicalParent = manifest.parentId() == null ? null : manifest.parentId().value();
        if (!Objects.equals(relationalParent, canonicalParent)
                || !manifest.createdAt().toString().equals(result.getString("created_at"))) {
            throw new StorageException("Stored revision lineage does not match canonical content");
        }
        return new StoredRevision(manifest, result.getLong("sequence"), storedHash);
    }
}

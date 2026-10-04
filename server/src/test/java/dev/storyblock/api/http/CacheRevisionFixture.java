package dev.storyblock.api.http;

import dev.storyblock.application.CommitService;
import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.time.Instant;
import java.util.Map;

public final class CacheRevisionFixture {
    public static RevisionManifest create(SqliteRevisionStore store) {
        var revision = NarrativeHttpFixture.revision(false);
        store.createNovel(revision,hash(revision)); return revision;
    }
    public static String hash(RevisionManifest revision) { return NarrativeHttpFixture.hash(revision); }
    public static RevisionManifest commit(SqliteRevisionStore store, RevisionManifest base) {
        var request = NarrativeHttpFixture.insertion(base);
        var operation = EditOperationCanonicalMapper.fromCanonical(CanonicalJson.bytes(request.get("operation")));
        var result = new CommitService(store).commit(operation,
                new Ids.RevisionId((String)request.get("candidate_revision_id")),
                Instant.parse((String)request.get("candidate_created_at")));
        return store.getRevision(base.novel().id(),result.revision().revisionId()).manifest();
    }
}

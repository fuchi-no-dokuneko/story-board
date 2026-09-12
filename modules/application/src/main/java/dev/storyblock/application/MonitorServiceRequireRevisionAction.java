package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredRevision;
import java.util.Objects;

final class MonitorServiceRequireRevisionAction {
    static StoredRevision requireRevision(MonitorService self, Ids.NovelId novelId, Ids.RevisionId revisionId, String revisionHash)  {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        Objects.requireNonNull(revisionHash, "revisionHash");
        StoredRevision revision = self.revisions.getRevision(novelId, revisionId);
        if (!revision.contentHash().equals(revisionHash)) {
            RevisionRef expected = new RevisionRef(
                    revisionId, revision.sequence(), revisionHash
            );
            throw new StaleHeadException(expected, revision.reference());
        }
        return revision;
    }
}

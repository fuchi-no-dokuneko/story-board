package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.StoredCheckpoint;
import dev.storyblock.storage.StoredRevision;

final class ReplayServiceReadCheckpointAction {
    static RevisionManifest readCheckpoint(ReplayService self, Ids.NovelId novelId, StoredCheckpoint checkpoint)  {
        CanonicalRevision canonical;
        try {
            canonical = CanonicalRevision.parseEnvelope(self.store.decompressCheckpoint(checkpoint));
        } catch (RuntimeException exception) {
            throw new ReplayException(
                    novelId,
                    checkpoint.sequence(),
                    "Checkpoint payload is invalid",
                    exception
            );
        }
        RevisionManifest revision = NarrativeCanonicalMapper.fromCanonical(canonical);
        StoredRevision relational = self.store.getRevisionAtSequence(novelId, checkpoint.sequence());
        if (!canonical.contentHash().equals(checkpoint.contentHash())
                || !revision.novel().id().equals(novelId)
                || !revision.id().equals(checkpoint.revisionId())
                || !relational.manifest().equals(revision)
                || !relational.contentHash().equals(checkpoint.contentHash())) {
            throw ReplayServiceFailure.failure(
                    novelId,
                    checkpoint.sequence(),
                    "Checkpoint identity or hash does not match canonical revision"
            );
        }
        return revision;
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.StoredCheckpoint;
import dev.storyblock.storage.StoredRevision;
import java.util.HashMap;
import java.util.Map;

final class ReplayServiceMaterializeAction {
    static ReplayResult materialize(ReplayService self, Ids.NovelId novelId, Ids.RevisionId targetRevisionId)  {
        StoredRevision target = self.store.getRevision(novelId, targetRevisionId);
        StoredCheckpoint checkpoint = self.store.loadCheckpoint(novelId, target.sequence())
                .orElseThrow(() -> ReplayServiceFailure.failure(
                        novelId, target.sequence(), "No checkpoint exists at or before target"
                ));
        RevisionManifest start = self.readCheckpoint(novelId, checkpoint);
        Map<Ids.RevisionId, RevisionManifest> restored = new HashMap<>();
        restored.put(start.id(), start);
        return self.replayRange(
                novelId,
                start,
                checkpoint.sequence(),
                target,
                revisionId -> restored.computeIfAbsent(
                        revisionId,
                        id -> self.materializeFull(novelId, id).revision()
                ),
                restored
        );
    }
}

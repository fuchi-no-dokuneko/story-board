package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.StoredRevision;
import java.util.HashMap;
import java.util.Map;

final class ReplayServiceMaterializeFullAction {
    static ReplayResult materializeFull(ReplayService self, Ids.NovelId novelId, Ids.RevisionId targetRevisionId)  {
        StoredRevision target = self.store.getRevision(novelId, targetRevisionId);
        StoredRevision genesis = self.store.getRevisionAtSequence(novelId, 0);
        if (genesis.manifest().parentId() != null) {
            throw ReplayServiceFailure.failure(novelId, 0, "Genesis revision has a parent");
        }
        Map<Ids.RevisionId, RevisionManifest> replayed = new HashMap<>();
        replayed.put(genesis.manifest().id(), genesis.manifest());
        RevisionLookup lookup = revisionId -> {
            RevisionManifest revision = replayed.get(revisionId);
            if (revision == null) {
                throw ReplayServiceFailure.failure(
                        novelId,
                        -1,
                        "Restore target was not present in replayed history: "
                                + revisionId.value()
                );
            }
            return revision;
        };
        return self.replayRange(novelId, genesis.manifest(), 0, target, lookup, replayed);
    }
}

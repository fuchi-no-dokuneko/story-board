package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredCheckpoint;
import dev.storyblock.storage.StoredRevision;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReplayService {
    final RevisionStore store;

    public ReplayService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public ReplayResult materialize(
            Ids.NovelId novelId,
            Ids.RevisionId targetRevisionId
    ) {
        return ReplayServiceMaterializeAction.materialize(this, novelId, targetRevisionId);
    }

    public ReplayResult materializeFull(
            Ids.NovelId novelId,
            Ids.RevisionId targetRevisionId
    ) {
        return ReplayServiceMaterializeFullAction.materializeFull(this, novelId, targetRevisionId);
    }

    public ReplayVerificationReport verifyAllHeads() {
        List<ReplayVerification> results = store.listNovels().stream()
                .map(this::verifyHead)
                .toList();
        return new ReplayVerificationReport(results);
    }

    ReplayVerification verifyHead(Ids.NovelId novelId) {
        return ReplayServiceVerifyHeadAction.verifyHead(this, novelId);
    }

    ReplayResult replayRange(
            Ids.NovelId novelId,
            RevisionManifest start,
            long startingSequence,
            StoredRevision target,
            RevisionLookup revisionLookup,
            Map<Ids.RevisionId, RevisionManifest> replayed
    ) {
        return ReplayServiceReplayRangeAction.replayRange(this, novelId, start, startingSequence, target, revisionLookup, replayed);
    }

    RevisionManifest readCheckpoint(
            Ids.NovelId novelId,
            StoredCheckpoint checkpoint
    ) {
        return ReplayServiceReadCheckpointAction.readCheckpoint(this, novelId, checkpoint);
    }

}

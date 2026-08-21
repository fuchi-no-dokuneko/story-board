package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredRevision;
import java.util.Objects;

public final class RenderService {
    private final RevisionStore store;
    private final DeterministicRenderer renderer;

    public RenderService(RevisionStore store) {
        this(store, new DeterministicRenderer());
    }

    public RenderService(RevisionStore store, DeterministicRenderer renderer) {
        this.store = Objects.requireNonNull(store, "store");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public RenderPacket render(
            Ids.NovelId novelId,
            Ids.RevisionId revisionId,
            String expectedHash,
            RenderRange range
    ) {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        Objects.requireNonNull(expectedHash, "expectedHash");
        Objects.requireNonNull(range, "range");

        StoredRevision revision = store.getRevision(novelId, revisionId);
        if (!revision.contentHash().equals(expectedHash)) {
            RevisionRef expected = new RevisionRef(
                    revisionId, revision.sequence(), expectedHash
            );
            throw new StaleHeadException(expected, revision.reference());
        }
        return renderer.render(revision.manifest(), revision.contentHash(), range);
    }
}

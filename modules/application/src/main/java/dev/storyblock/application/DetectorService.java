package dev.storyblock.application;

import dev.storyblock.detector.AdjacentMetadataDetector;
import dev.storyblock.detector.DetectorRun;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredRevision;
import java.util.Objects;

public final class DetectorService {
    private final RevisionStore store;
    private final AdjacentMetadataDetector detector;

    public DetectorService(RevisionStore store) {
        this(store, new AdjacentMetadataDetector());
    }

    public DetectorService(RevisionStore store, AdjacentMetadataDetector detector) {
        this.store = Objects.requireNonNull(store, "store");
        this.detector = Objects.requireNonNull(detector, "detector");
    }

    public DetectorRun detect(
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
        return detector.detect(revision.manifest(), revision.contentHash(), range);
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.DeterministicPdfRenderer;
import dev.storyblock.renderer.PdfRenderResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredArtifact;
import dev.storyblock.storage.StoredRevision;
import java.util.Objects;

public final class PdfRenderService {
    private final RevisionStore store;
    private final DeterministicPdfRenderer renderer;

    public PdfRenderService(RevisionStore store) {
        this(store, new DeterministicPdfRenderer());
    }

    PdfRenderService(RevisionStore store, DeterministicPdfRenderer renderer) {
        this.store = Objects.requireNonNull(store, "store");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public PdfRenderResult render(
            Ids.NovelId novelId,
            Ids.RevisionId revisionId,
            String expectedHash
    ) {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        Objects.requireNonNull(expectedHash, "expectedHash");
        StoredRevision revision = store.getRevision(novelId, revisionId);
        if (!revision.contentHash().equals(expectedHash)) {
            throw new StaleHeadException(
                    new RevisionRef(revisionId, revision.sequence(), expectedHash),
                    revision.reference()
            );
        }
        return renderer.render(
                revision.manifest(),
                revision.contentHash(),
                image -> resolveImage(novelId, image)
        );
    }

    private byte[] resolveImage(Ids.NovelId novelId, BlockImage image) {
        StoredArtifact artifact = store.getArtifact(image.artifactId());
        if (!artifact.novelId().equals(novelId)
                || !artifact.portable()
                || !"narrative-image".equals(artifact.kind())
                || !artifact.mediaType().equals(image.mediaType())
                || !artifact.contentHash().equals(image.contentHash())) {
            throw new IllegalArgumentException(
                    "Image block references a mismatched novel artifact"
            );
        }
        return artifact.content();
    }
}

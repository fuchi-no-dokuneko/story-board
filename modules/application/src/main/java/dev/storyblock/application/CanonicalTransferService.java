package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.CanonicalImportResult;
import dev.storyblock.storage.ExportJobResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredArtifact;
import dev.storyblock.storage.StoredExportJob;
import dev.storyblock.storage.StoredRevision;
import java.time.Instant;
import java.util.Objects;

public final class CanonicalTransferService {
    static final String REVISION_MEDIA_TYPE =
            "application/vnd.storyblock.revision+json";
    static final String PACKAGE_MEDIA_TYPE =
            "application/vnd.storyblock.package+json";

    final RevisionStore store;

    public CanonicalTransferService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public CanonicalImportResult importDocument(
            CanonicalExportFormat format,
            byte[] document,
            String idempotencyKey,
            Instant importedAt
    ) {
        return CanonicalTransferServiceImportDocumentAction.importDocument(this, format, document, idempotencyKey, importedAt);
    }

    public byte[] exportPackage(Ids.NovelId novelId) {
        CanonicalNovelPackage document = store.loadCanonicalPackage(novelId);
        verifyReplay(document);
        return document.bytes();
    }

    public ExportJobResult requestExport(
            Ids.NovelId novelId,
            Ids.RevisionId revisionId,
            String expectedHash,
            CanonicalExportFormat format,
            String idempotencyKey,
            Instant requestedAt
    ) {
        return CanonicalTransferServiceRequestExportAction.requestExport(this, novelId, revisionId, expectedHash, format, idempotencyKey, requestedAt);
    }

    public StoredExportJob getExportJob(Ids.JobId jobId) {
        return store.getExportJob(jobId);
    }

    public StoredArtifact getArtifact(Ids.ArtifactId artifactId) {
        return store.getArtifact(artifactId);
    }

    public RevisionRef getHead(Ids.NovelId novelId) {
        return store.getHead(novelId);
    }

    public StoredRevision getRevision(
            Ids.NovelId novelId,
            Ids.RevisionId revisionId
    ) {
        return store.getRevision(novelId, revisionId);
    }

    public static void verifyReplay(CanonicalNovelPackage document) {
        CanonicalTransferServiceVerifyReplayFactory.verifyReplay(document);
    }

    record VerifiedImageArtifact(
            CanonicalNovelPackage.ArtifactEntry artifact,
            ImageUploadService.ImageInfo decoded
    ) {
    }
}

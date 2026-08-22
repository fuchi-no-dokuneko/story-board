package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.CanonicalImportRequest;
import dev.storyblock.storage.CanonicalImportResult;
import dev.storyblock.storage.ExportJobRequest;
import dev.storyblock.storage.ExportJobResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredArtifact;
import dev.storyblock.storage.StoredExportJob;
import dev.storyblock.storage.StoredRevision;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CanonicalTransferService {
    private static final String REVISION_MEDIA_TYPE =
            "application/vnd.storyblock.revision+json";
    private static final String PACKAGE_MEDIA_TYPE =
            "application/vnd.storyblock.package+json";

    private final RevisionStore store;

    public CanonicalTransferService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public CanonicalImportResult importDocument(
            CanonicalExportFormat format,
            byte[] document,
            String idempotencyKey,
            Instant importedAt
    ) {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(document, "document");
        CanonicalNovelPackage canonicalPackage = switch (format) {
            case REVISION -> CanonicalNovelPackage.genesis(
                    CanonicalRevision.parseEnvelope(document)
            );
            case PACKAGE -> CanonicalNovelPackage.parse(document);
        };
        verifyReplay(canonicalPackage);
        String requestHash = CanonicalJson.hash(Map.of(
                "format", format.canonicalName(),
                "document_hash", format == CanonicalExportFormat.PACKAGE
                        ? canonicalPackage.packageHash()
                        : canonicalPackage.manifest().headHash()
        ));
        return store.importCanonicalPackage(new CanonicalImportRequest(
                canonicalPackage,
                idempotencyKey,
                requestHash,
                importedAt
        ));
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
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(requestedAt, "requestedAt");

        RevisionRef observedHead = store.getHead(novelId);
        RevisionRef expectedHead = new RevisionRef(
                revisionId, observedHead.sequence(), expectedHash
        );
        byte[] content = switch (format) {
            case REVISION -> NarrativeCanonicalMapper.toCanonical(
                    store.getRevision(novelId, revisionId).manifest()
            ).envelopeBytes();
            case PACKAGE -> exportPackage(novelId);
        };
        String requestHash = CanonicalJson.hash(Map.of(
                "format", format.canonicalName(),
                "novel_id", novelId.value(),
                "revision_id", revisionId.value(),
                "revision_hash", expectedHash
        ));
        StoredArtifact artifact = new StoredArtifact(
                Ids.ArtifactId.create(),
                novelId,
                revisionId,
                format.canonicalName(),
                format == CanonicalExportFormat.PACKAGE
                        ? PACKAGE_MEDIA_TYPE : REVISION_MEDIA_TYPE,
                "identity",
                CanonicalJson.hashBytes(content),
                content,
                requestedAt,
                false
        );
        return store.createCompletedExport(new ExportJobRequest(
                Ids.JobId.create(),
                novelId,
                expectedHead,
                format,
                idempotencyKey,
                requestHash,
                artifact,
                requestedAt
        ));
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
        Objects.requireNonNull(document, "document");
        Map<Ids.RevisionId, RevisionManifest> materialized = new LinkedHashMap<>();
        RevisionManifest current = NarrativeCanonicalMapper.fromCanonical(
                document.revisions().getFirst().revision()
        );
        materialized.put(current.id(), current);

        NarrativeEditor editor = new NarrativeEditor(revisionId -> {
            RevisionManifest revision = materialized.get(revisionId);
            if (revision == null) {
                throw new CanonicalPackageException(
                        "Operation references an unavailable historical revision "
                                + revisionId.value()
                );
            }
            return revision;
        });
        for (int index = 0; index < document.operations().size(); index++) {
            CanonicalNovelPackage.OperationEntry operation = document.operations().get(index);
            CanonicalNovelPackage.RevisionEntry expected = document.revisions().get(index + 1);
            final RevisionManifest candidate;
            try {
                candidate = editor.apply(
                        current,
                        operation.operation(),
                        operation.resultRevisionId(),
                        operation.committedAt()
                );
            } catch (RuntimeException failure) {
                throw new CanonicalPackageException(
                        "Canonical operation replay failed at sequence " + operation.sequence(),
                        failure
                );
            }
            CanonicalRevision replayed = NarrativeCanonicalMapper.toCanonical(candidate);
            if (!MessageDigest.isEqual(replayed.envelopeBytes(), expected.revision().envelopeBytes())) {
                throw new CanonicalPackageException(
                        "Canonical operation replay drift at sequence " + operation.sequence()
                );
            }
            materialized.put(candidate.id(), candidate);
            current = candidate;
        }
        if (!NarrativeCanonicalMapper.toCanonical(current).contentHash().equals(
                document.manifest().headHash()
        )) {
            throw new CanonicalPackageException("Full package replay does not match the head hash");
        }
    }
}

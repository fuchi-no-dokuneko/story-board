package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockImage;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
        StoredRevision selectedRevision = store.getRevision(novelId, revisionId);
        byte[] content = switch (format) {
            case REVISION -> {
                if (containsImage(selectedRevision.manifest())) {
                    throw new CanonicalPackageException(
                            "Image-bearing revisions require canonical-package export"
                    );
                }
                yield NarrativeCanonicalMapper.toCanonical(
                        selectedRevision.manifest()
                ).envelopeBytes();
            }
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
        List<RevisionManifest> manifests = new ArrayList<>();
        RevisionManifest current = NarrativeCanonicalMapper.fromCanonical(
                document.revisions().getFirst().revision()
        );
        materialized.put(current.id(), current);
        manifests.add(current);

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
            manifests.add(candidate);
            current = candidate;
        }
        if (!NarrativeCanonicalMapper.toCanonical(current).contentHash().equals(
                document.manifest().headHash()
        )) {
            throw new CanonicalPackageException("Full package replay does not match the head hash");
        }
        validateImageArtifacts(document, manifests);
    }

    private static void validateImageArtifacts(
            CanonicalNovelPackage document,
            List<RevisionManifest> manifests
    ) {
        Map<Ids.RevisionId, Integer> revisionSequence = new HashMap<>();
        for (int index = 0; index < manifests.size(); index++) {
            revisionSequence.put(manifests.get(index).id(), index);
        }

        Map<Ids.ArtifactId, VerifiedImageArtifact> images = new HashMap<>();
        for (CanonicalNovelPackage.ArtifactEntry artifact : document.artifacts()) {
            if (!"narrative-image".equals(artifact.kind())) {
                continue;
            }
            if (!"identity".equals(artifact.codec())
                    || artifact.content().length == 0
                    || artifact.content().length > ImageUploadService.MAX_IMAGE_BYTES) {
                throw new CanonicalPackageException(
                        "Narrative image artifact has an invalid codec or byte size"
                );
            }
            final ImageUploadService.ImageInfo decoded;
            try {
                decoded = ImageUploadService.inspect(artifact.content());
            } catch (IllegalArgumentException failure) {
                throw new CanonicalPackageException(
                        "Narrative image artifact cannot be decoded safely", failure
                );
            }
            if (!decoded.mediaType().equals(artifact.mediaType())) {
                throw new CanonicalPackageException(
                        "Narrative image artifact media type does not match its bytes"
                );
            }
            images.put(artifact.artifactId(), new VerifiedImageArtifact(artifact, decoded));
        }

        for (int revisionIndex = 0; revisionIndex < manifests.size(); revisionIndex++) {
            RevisionManifest revision = manifests.get(revisionIndex);
            for (var chapter : revision.novel().chapters()) {
                for (var scene : chapter.scenes()) {
                    for (var block : scene.blocks()) {
                        if (block.image().isEmpty()) {
                            continue;
                        }
                        BlockImage descriptor = block.image().orElseThrow();
                        VerifiedImageArtifact verified = images.get(descriptor.artifactId());
                        if (verified == null
                                || revisionSequence.get(verified.artifact().revisionId())
                                        > revisionIndex
                                || !verified.artifact().contentHash().equals(
                                        descriptor.contentHash()
                                )
                                || !verified.artifact().mediaType().equals(
                                        descriptor.mediaType()
                                )
                                || verified.decoded().widthPixels()
                                        != descriptor.widthPixels()
                                || verified.decoded().heightPixels()
                                        != descriptor.heightPixels()) {
                            throw new CanonicalPackageException(
                                    "Image block does not match an available portable artifact"
                            );
                        }
                    }
                }
            }
        }
    }

    private static boolean containsImage(RevisionManifest revision) {
        return revision.novel().chapters().stream()
                .flatMap(chapter -> chapter.scenes().stream())
                .flatMap(scene -> scene.blocks().stream())
                .anyMatch(block -> block.image().isPresent());
    }

    private record VerifiedImageArtifact(
            CanonicalNovelPackage.ArtifactEntry artifact,
            ImageUploadService.ImageInfo decoded
    ) {
    }
}

package dev.storyblock.application;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.*;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

final class CanonicalTransferServiceRequestExportAction {
  static ExportJobResult requestExport(CanonicalTransferService self, Ids.NovelId novelId, Ids.RevisionId revisionId, String expectedHash, CanonicalExportFormat format, String idempotencyKey, Instant requestedAt)  {
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(revisionId, "revisionId");
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(requestedAt, "requestedAt");

    RevisionRef observedHead = self.store.getHead(novelId);
    RevisionRef expectedHead = new RevisionRef(
        revisionId, observedHead.sequence(), expectedHash
    );
    StoredRevision selectedRevision = self.store.getRevision(novelId, revisionId);
    byte[] content = switch (format) {
      case REVISION -> {
        if (CanonicalTransferServiceContainsImage.containsImage(selectedRevision.manifest())) {
          throw new CanonicalPackageException(
              "Image-bearing revisions require canonical-package export"
          );
        }
        yield NarrativeCanonicalMapper.toCanonical(
            selectedRevision.manifest()
        ).envelopeBytes();
      }
      case PACKAGE -> self.exportPackage(novelId);
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
            ? CanonicalTransferService.PACKAGE_MEDIA_TYPE : CanonicalTransferService.REVISION_MEDIA_TYPE,
        "identity",
        CanonicalJson.hashBytes(content),
        content,
        requestedAt,
        false
    );
    return self.store.createCompletedExport(new ExportJobRequest(
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
}

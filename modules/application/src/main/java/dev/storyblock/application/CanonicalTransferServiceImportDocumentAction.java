package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.storage.CanonicalImportRequest;
import dev.storyblock.storage.CanonicalImportResult;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

final class CanonicalTransferServiceImportDocumentAction {
    static CanonicalImportResult importDocument(CanonicalTransferService self, CanonicalExportFormat format, byte[] document, String idempotencyKey, Instant importedAt)  {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(document, "document");
        CanonicalNovelPackage canonicalPackage = switch (format) {
            case REVISION -> CanonicalNovelPackage.genesis(
                    CanonicalRevision.parseEnvelope(document)
            );
            case PACKAGE -> CanonicalNovelPackage.parse(document);
        };
        CanonicalTransferService.verifyReplay(canonicalPackage);
        String requestHash = CanonicalJson.hash(Map.of(
                "format", format.canonicalName(),
                "document_hash", format == CanonicalExportFormat.PACKAGE
                        ? canonicalPackage.packageHash()
                        : canonicalPackage.manifest().headHash()
        ));
        return self.store.importCanonicalPackage(new CanonicalImportRequest(
                canonicalPackage,
                idempotencyKey,
                requestHash,
                importedAt
        ));
    }
}

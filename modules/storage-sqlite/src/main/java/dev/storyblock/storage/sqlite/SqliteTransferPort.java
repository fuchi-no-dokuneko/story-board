package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.util.Objects;

interface SqliteTransferPort extends RevisionStore, SqliteStoreContext {
  @Override
  default dev.storyblock.contracts.CanonicalNovelPackage loadCanonicalPackage(
      Ids.NovelId novelId
  ) {
    return context().read(connection -> SqliteTransferLoadPackage.loadPackage(connection, novelId));
  }

  @Override
  default CanonicalImportResult importCanonicalPackage(CanonicalImportRequest request) {
    Objects.requireNonNull(request, "request");
    return context().write(connection -> SqliteTransferImportPackage.importPackage(
        connection, request, context().importFaultInjector
    ));
  }

  @Override
  default ExportJobResult createCompletedExport(ExportJobRequest request) {
    Objects.requireNonNull(request, "request");
    return context().write(connection -> SqliteTransferCreateCompletedExport.createCompletedExport(
        connection, request
    ));
  }

  @Override
  default StoredExportJob getExportJob(Ids.JobId jobId) {
    return context().read(connection -> SqliteTransferGetExportJob.getExportJob(connection, jobId));
  }

  @Override
  default StoredArtifact getArtifact(Ids.ArtifactId artifactId) {
    return context().read(connection -> SqliteTransferGetArtifact.getArtifact(connection, artifactId));
  }

  @Override
  default PortableArtifactPutResult putPortableArtifact(PortableArtifactPutRequest request) {
    Objects.requireNonNull(request, "request");
    return context().write(connection -> SqliteTransferPutPortableArtifact.putPortableArtifact(
        connection, request
    ));
  }
}

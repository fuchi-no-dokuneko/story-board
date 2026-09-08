package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.ExportJobRequest;
import dev.storyblock.storage.StoredExportJob;

final class SqliteTransferToStoredJob {
    static StoredExportJob toStoredJob(ExportJobRequest request) {
        return new StoredExportJob(
                request.jobId(),
                request.novelId(),
                request.expectedHead(),
                request.format(),
                request.artifact().artifactId(),
                request.createdAt()
        );
    }
}

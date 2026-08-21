package dev.storyblock.storage;

import dev.storyblock.domain.Ids;

public final class MissingExportJobException extends StorageException {
    public MissingExportJobException(Ids.JobId jobId) {
        super("No stored export job " + jobId.value());
    }
}

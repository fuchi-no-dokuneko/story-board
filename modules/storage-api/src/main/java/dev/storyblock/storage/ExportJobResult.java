package dev.storyblock.storage;

import java.util.Objects;

public record ExportJobResult(
        StoredExportJob job,
        boolean idempotentReplay
) {
    public ExportJobResult {
        Objects.requireNonNull(job, "job");
    }
}

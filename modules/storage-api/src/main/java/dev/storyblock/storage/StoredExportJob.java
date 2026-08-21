package dev.storyblock.storage;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;

public record StoredExportJob(
        Ids.JobId jobId,
        Ids.NovelId novelId,
        RevisionRef revision,
        CanonicalExportFormat format,
        Ids.ArtifactId resultArtifactId,
        Instant createdAt
) {
    public static final String KIND = "canonical-export";
    public static final String STATUS = "succeeded";
    public static final int ATTEMPT = 1;

    public StoredExportJob {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(resultArtifactId, "resultArtifactId");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}

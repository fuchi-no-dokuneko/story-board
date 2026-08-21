package dev.storyblock.storage;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record ExportJobRequest(
        Ids.JobId jobId,
        Ids.NovelId novelId,
        RevisionRef expectedHead,
        CanonicalExportFormat format,
        String idempotencyKey,
        String requestHash,
        StoredArtifact artifact,
        Instant createdAt
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public ExportJobRequest {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(expectedHead, "expectedHead");
        Objects.requireNonNull(format, "format");
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("Export idempotency key is invalid");
        }
        if (requestHash == null || !SHA_256.matcher(requestHash).matches()) {
            throw new IllegalArgumentException("Export request hash must be lowercase SHA-256");
        }
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(createdAt, "createdAt");
        if (!artifact.novelId().equals(novelId)
                || !artifact.revisionId().equals(expectedHead.revisionId())
                || artifact.portable()) {
            throw new IllegalArgumentException(
                    "Export artifact must be a generated artifact for the expected head"
            );
        }
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredArtifact;
import java.time.Instant;
import java.util.Objects;

public final class ImageUploadService {
    public static final int MAX_IMAGE_BYTES = 1_500_000;

    final RevisionStore store;

    public ImageUploadService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public Result upload(
            Ids.NovelId novelId,
            String expectedHeadHash,
            String idempotencyKey,
            byte[] content,
            Instant createdAt
    ) {
        return ImageUploadServiceUploadAction.upload(this, novelId, expectedHeadHash, idempotencyKey, content, createdAt);
    }

    static ImageInfo inspect(byte[] content) {
        return ImageUploadServiceInspectFactory.inspect(content);
    }

    record ImageInfo(String mediaType, int widthPixels, int heightPixels) {
    }

    public record Result(
            StoredArtifact artifact,
            int widthPixels,
            int heightPixels,
            boolean idempotentReplay
    ) {
        public Result {
            Objects.requireNonNull(artifact, "artifact");
        }
    }
}

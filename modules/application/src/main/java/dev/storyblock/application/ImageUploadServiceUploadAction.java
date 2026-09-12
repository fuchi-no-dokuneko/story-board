package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.StableIds;
import dev.storyblock.storage.PortableArtifactPutRequest;
import dev.storyblock.storage.PortableArtifactPutResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredArtifact;
import java.time.Instant;
import java.util.Objects;
import static dev.storyblock.application.ImageUploadService.ImageInfo;
import static dev.storyblock.application.ImageUploadService.Result;

final class ImageUploadServiceUploadAction {
    static Result upload(ImageUploadService self, Ids.NovelId novelId, String expectedHeadHash, String idempotencyKey, byte[] content, Instant createdAt)  {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(expectedHeadHash, "expectedHeadHash");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(createdAt, "createdAt");
        byte[] safeContent = Objects.requireNonNull(content, "content").clone();
        if (safeContent.length == 0) {
            throw new IllegalArgumentException(
                    "Image must contain 1 to " + ImageUploadService.MAX_IMAGE_BYTES + " bytes"
            );
        }
        if (safeContent.length > ImageUploadService.MAX_IMAGE_BYTES) {
            throw new ImagePayloadTooLargeException(ImageUploadService.MAX_IMAGE_BYTES);
        }

        ImageInfo info = ImageUploadService.inspect(safeContent);
        RevisionRef head = self.store.getHead(novelId);
        if (!head.contentHash().equals(expectedHeadHash)) {
            throw new StaleHeadException(
                    new RevisionRef(head.revisionId(), head.sequence(), expectedHeadHash),
                    head
            );
        }
        Ids.ArtifactId artifactId = new Ids.ArtifactId(StableIds.derive(
                "art", novelId.value(), "image-upload:" + idempotencyKey
        ));
        StoredArtifact artifact = new StoredArtifact(
                artifactId,
                novelId,
                head.revisionId(),
                "narrative-image",
                info.mediaType(),
                "identity",
                CanonicalJson.hashBytes(safeContent),
                safeContent,
                createdAt,
                true
        );
        PortableArtifactPutResult stored = self.store.putPortableArtifact(
                new PortableArtifactPutRequest(head, idempotencyKey, artifact)
        );
        return new Result(
                stored.artifact(),
                info.widthPixels(),
                info.heightPixels(),
                stored.idempotentReplay()
        );
    }
}

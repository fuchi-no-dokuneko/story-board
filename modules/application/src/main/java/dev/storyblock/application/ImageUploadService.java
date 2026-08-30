package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.StableIds;
import dev.storyblock.storage.PortableArtifactPutRequest;
import dev.storyblock.storage.PortableArtifactPutResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredArtifact;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public final class ImageUploadService {
    public static final int MAX_IMAGE_BYTES = 1_500_000;

    private final RevisionStore store;

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
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(expectedHeadHash, "expectedHeadHash");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(createdAt, "createdAt");
        byte[] safeContent = Objects.requireNonNull(content, "content").clone();
        if (safeContent.length == 0) {
            throw new IllegalArgumentException(
                    "Image must contain 1 to " + MAX_IMAGE_BYTES + " bytes"
            );
        }
        if (safeContent.length > MAX_IMAGE_BYTES) {
            throw new ImagePayloadTooLargeException(MAX_IMAGE_BYTES);
        }

        ImageInfo info = inspect(safeContent);
        RevisionRef head = store.getHead(novelId);
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
        PortableArtifactPutResult stored = store.putPortableArtifact(
                new PortableArtifactPutRequest(head, idempotencyKey, artifact)
        );
        return new Result(
                stored.artifact(),
                info.widthPixels(),
                info.heightPixels(),
                stored.idempotentReplay()
        );
    }

    static ImageInfo inspect(byte[] content) {
        String expectedMediaType;
        if (isPng(content)) {
            expectedMediaType = "image/png";
        } else if (isJpeg(content)) {
            expectedMediaType = "image/jpeg";
        } else {
            throw new IllegalArgumentException("Only PNG and JPEG image uploads are supported");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(content)
        )) {
            if (input == null) {
                throw new IllegalArgumentException("Image input could not be decoded");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Image input could not be decoded");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || width > 8_192 || height < 1 || height > 8_192
                        || (long) width * height > 40_000_000L) {
                    throw new IllegalArgumentException(
                            "Image dimensions exceed the safety limit"
                    );
                }
                return new ImageInfo(expectedMediaType, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException failure) {
            throw new IllegalArgumentException("Image input could not be decoded", failure);
        }
    }

    private static boolean isPng(byte[] content) {
        byte[] signature = {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        };
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJpeg(byte[] content) {
        return content.length >= 3
                && content[0] == (byte) 0xff
                && content[1] == (byte) 0xd8
                && content[2] == (byte) 0xff;
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

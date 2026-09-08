package dev.storyblock.application;

import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredArtifact;

final class CommitServiceValidateImageReferenceAction {
    static void validateImageReference(CommitService self, Ids.NovelId novelId, BlockImage image)  {
        StoredArtifact artifact = self.store.getArtifact(image.artifactId());
        ImageUploadService.ImageInfo decoded = ImageUploadService.inspect(
                artifact.content()
        );
        if (!artifact.novelId().equals(novelId)
                || !artifact.portable()
                || !"narrative-image".equals(artifact.kind())
                || !artifact.mediaType().equals(image.mediaType())
                || !artifact.contentHash().equals(image.contentHash())
                || !decoded.mediaType().equals(image.mediaType())
                || decoded.widthPixels() != image.widthPixels()
                || decoded.heightPixels() != image.heightPixels()) {
            throw new IllegalArgumentException(
                    "Image block must reference a matching portable image artifact in this novel"
            );
        }
    }
}

package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.domain.*;
import java.util.HashMap;
import java.util.Map;
import static dev.storyblock.application.CanonicalTransferService.VerifiedImageArtifact;

final class PortableImageInspection {
  static Map<Ids.ArtifactId, VerifiedImageArtifact> inspect(CanonicalNovelPackage document) {
    Map<Ids.ArtifactId, VerifiedImageArtifact> images = new HashMap<>();
    for (CanonicalNovelPackage.ArtifactEntry artifact : document.artifacts()) {
      if (!"narrative-image".equals(artifact.kind())) {
        continue;
      }
      if (!"identity".equals(artifact.codec())
          || artifact.content().length == 0
          || artifact.content().length > ImageUploadService.MAX_IMAGE_BYTES) {
        throw new CanonicalPackageException(
            "Narrative image artifact has an invalid codec or byte size"
        );
      }
      final ImageUploadService.ImageInfo decoded;
      try {
        decoded = ImageUploadService.inspect(artifact.content());
      } catch (IllegalArgumentException failure) {
        throw new CanonicalPackageException(
            "Narrative image artifact cannot be decoded safely", failure
        );
      }
      if (!decoded.mediaType().equals(artifact.mediaType())) {
        throw new CanonicalPackageException(
            "Narrative image artifact media type does not match its bytes"
        );
      }
      images.put(artifact.artifactId(), new VerifiedImageArtifact(artifact, decoded));
    }

    return images;
  }
}

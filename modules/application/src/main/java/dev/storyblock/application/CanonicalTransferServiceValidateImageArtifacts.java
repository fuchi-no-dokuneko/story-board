package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.domain.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.application.CanonicalTransferService.VerifiedImageArtifact;

final class CanonicalTransferServiceValidateImageArtifacts {
  static void validateImageArtifacts(CanonicalNovelPackage document, List<RevisionManifest> manifests)  {
    Map<Ids.RevisionId, Integer> revisionSequence = new HashMap<>();
    for (int index = 0; index < manifests.size(); index++) {
      revisionSequence.put(manifests.get(index).id(), index);
    }

    Map<Ids.ArtifactId, VerifiedImageArtifact> images = PortableImageInspection.inspect(document);

    for (int revisionIndex = 0; revisionIndex < manifests.size(); revisionIndex++) {
      RevisionManifest revision = manifests.get(revisionIndex);
      for (var chapter : revision.novel().chapters()) {
        for (var scene : chapter.scenes()) {
          for (var block : scene.blocks()) {
            if (block.image().isEmpty()) {
              continue;
            }
            BlockImage descriptor = block.image().orElseThrow();
            VerifiedImageArtifact verified = images.get(descriptor.artifactId());
            if (verified == null
                || revisionSequence.get(verified.artifact().revisionId())
                    > revisionIndex
                || !verified.artifact().contentHash().equals(
                    descriptor.contentHash()
                )
                || !verified.artifact().mediaType().equals(
                    descriptor.mediaType()
                )
                || verified.decoded().widthPixels()
                    != descriptor.widthPixels()
                || verified.decoded().heightPixels()
                    != descriptor.heightPixels()) {
              throw new CanonicalPackageException(
                  "Image block does not match an available portable artifact"
              );
            }
          }
        }
      }
    }
  }
}

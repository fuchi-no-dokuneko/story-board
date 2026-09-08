package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.application.CanonicalTransferService.VerifiedImageArtifact;

final class CanonicalTransferServiceValidateImageArtifacts {
    static void validateImageArtifacts(
            CanonicalNovelPackage document,
            List<RevisionManifest> manifests
    ) {
        Map<Ids.RevisionId, Integer> revisionSequence = new HashMap<>();
        for (int index = 0; index < manifests.size(); index++) {
            revisionSequence.put(manifests.get(index).id(), index);
        }

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

package dev.storyblock.application;

import dev.storyblock.domain.RevisionManifest;

final class CanonicalTransferServiceContainsImage {
    static boolean containsImage(RevisionManifest revision) {
        return revision.novel().chapters().stream()
                .flatMap(chapter -> chapter.scenes().stream())
                .flatMap(scene -> scene.blocks().stream())
                .anyMatch(block -> block.image().isPresent());
    }
}

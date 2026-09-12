package dev.storyblock.renderer;

import dev.storyblock.domain.BlockImage;

@FunctionalInterface
public interface PdfImageResolver {
    byte[] resolve(BlockImage image);
}

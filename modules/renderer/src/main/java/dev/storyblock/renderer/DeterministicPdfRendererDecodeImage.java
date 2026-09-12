package dev.storyblock.renderer;

import dev.storyblock.domain.BlockImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import static dev.storyblock.renderer.DeterministicPdfRenderer.SUPPORTED_IMAGE_TYPES;

final class DeterministicPdfRendererDecodeImage {
    static BufferedImage decodeImage(BlockImage descriptor, byte[] content) {
        Objects.requireNonNull(content, "Image resolver returned null");
        if (!SUPPORTED_IMAGE_TYPES.contains(descriptor.mediaType())
                || !DeterministicPdfRendererHash.hash(content).equals(descriptor.contentHash())) {
            throw new IllegalArgumentException("Resolved image does not match its block descriptor");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(content)
        )) {
            if (input == null) {
                throw new IllegalArgumentException("Resolved image cannot be decoded");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Resolved image cannot be decoded");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width != descriptor.widthPixels() || height != descriptor.heightPixels()
                        || (long) width * height > 40_000_000L) {
                    throw new IllegalArgumentException(
                            "Resolved image dimensions do not match its block descriptor"
                    );
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("Resolved image cannot be decoded");
                }
                return decoded;
            } finally {
                reader.dispose();
            }
        } catch (IOException failure) {
            throw new IllegalArgumentException("Resolved image cannot be decoded", failure);
        }
    }
}

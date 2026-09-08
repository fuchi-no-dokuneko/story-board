package dev.storyblock.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import static dev.storyblock.application.ImageUploadService.ImageInfo;

final class ImageUploadServiceInspectFactory {
    static ImageInfo inspect(byte[] content)  {
        String expectedMediaType;
        if (ImageUploadServiceIsPng.isPng(content)) {
            expectedMediaType = "image/png";
        } else if (ImageUploadServiceIsJpeg.isJpeg(content)) {
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
}

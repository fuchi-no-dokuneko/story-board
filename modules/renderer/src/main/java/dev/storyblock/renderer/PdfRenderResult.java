package dev.storyblock.renderer;

import java.util.Objects;

public record PdfRenderResult(
        byte[] content,
        int pageCount,
        int imageCount,
        String rendererVersion
) {
    public PdfRenderResult {
        content = Objects.requireNonNull(content, "content").clone();
        if (pageCount < 1 || imageCount < 0) {
            throw new IllegalArgumentException("PDF render counts are invalid");
        }
        Objects.requireNonNull(rendererVersion, "rendererVersion");
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}

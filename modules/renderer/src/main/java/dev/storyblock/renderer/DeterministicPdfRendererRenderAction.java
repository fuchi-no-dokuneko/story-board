package dev.storyblock.renderer;

import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

final class DeterministicPdfRendererRenderAction {
    static PdfRenderResult render(DeterministicPdfRenderer self, RevisionManifest revision, String revisionHash, PdfImageResolver imageResolver)  {
        Objects.requireNonNull(revision, "revision");
        if (revisionHash == null || !DeterministicPdfRenderer.SHA_256.matcher(revisionHash).matches()) {
            throw new IllegalArgumentException("Revision hash must be lowercase SHA-256");
        }
        Objects.requireNonNull(imageResolver, "imageResolver");

        String title = DeterministicPdfRendererTitle.title(revision);
        Font bodyFont = DeterministicPdfRendererChooseFont.chooseFont(DeterministicPdfRendererVisibleText.visibleText(revision, title), Font.PLAIN, DeterministicPdfRenderer.BODY_SIZE);
        Font chapterFont = bodyFont.deriveFont(Font.BOLD, 30f);
        Font titleFont = bodyFont.deriveFont(Font.BOLD, 42f);
        PdfPageComposer pages = new PdfPageComposer(bodyFont, chapterFont, titleFont);
        pages.cover(title, revision.id().value());

        int imageCount = 0;
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            pages.chapter(chapter.title() == null ? "Untitled chapter" : chapter.title());
            for (NarrativeScene scene : chapter.scenes()) {
                if (scene.title() != null && !scene.title().equals(chapter.title())) {
                    pages.scene(scene.title());
                }
                for (NarrativeBlock block : scene.blocks()) {
                    if (block.image().isPresent()) {
                        BlockImage descriptor = block.image().orElseThrow();
                        pages.image(DeterministicPdfRendererDecodeImage.decodeImage(descriptor, imageResolver.resolve(descriptor)));
                        pages.caption(block.text());
                        imageCount++;
                    } else {
                        pages.paragraph(block.text());
                    }
                }
            }
        }
        List<BufferedImage> renderedPages = pages.finish();
        byte[] pdf = PdfDocumentWriter.write(renderedPages);
        return new PdfRenderResult(pdf, renderedPages.size(), imageCount, DeterministicPdfRenderer.VERSION);
    }
}

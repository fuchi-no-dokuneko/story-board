package dev.storyblock.renderer;

import java.awt.Font;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;
import static org.junit.jupiter.api.Assertions.*;

class PdfPaginationFontTest {
    @Test void paragraphKeepsItsFontAcrossPageBreak() {
        assertSamePixels(page -> page.paragraph("列車進站時，雨已經停了。她將照片留在筆記裡。"));
    }

    @Test void captionKeepsItsFontAcrossPageBreak() {
        assertSamePixels(page -> page.caption("候車室的燈光與河岸舊照片。"));
    }

    private static void assertSamePixels(Consumer<PdfPageComposer> draw) {
        var first = composer();
        var next = composer();
        next.cursorY = PAGE_HEIGHT - MARGIN_BOTTOM;
        draw.accept(first);
        draw.accept(next);
        assertEquals(2, next.pages.size());
        var expected = first.finish().getFirst();
        var actual = next.finish().getLast();
        int height = first.cursorY - MARGIN_TOP;
        assertArrayEquals(
            expected.getRGB(0, MARGIN_TOP, PAGE_WIDTH, height, null, 0, PAGE_WIDTH),
            actual.getRGB(0, MARGIN_TOP, PAGE_WIDTH, height, null, 0, PAGE_WIDTH)
        );
    }

    private static PdfPageComposer composer() {
        var body = BundledPdfFont.at(Font.PLAIN, BODY_SIZE);
        return new PdfPageComposer(body, body.deriveFont(28f), body.deriveFont(36f));
    }
}

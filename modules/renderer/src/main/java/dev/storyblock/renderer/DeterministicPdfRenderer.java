package dev.storyblock.renderer;

import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.UnicodeText;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/** Deterministically lays out a revision and packages rasterized A4 pages as PDF. */
public final class DeterministicPdfRenderer {
    public static final String VERSION = "pdf-renderer-1.0.0";

    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final int PAGE_WIDTH = 794;
    private static final int PAGE_HEIGHT = 1_123;
    private static final int MARGIN_X = 72;
    private static final int MARGIN_TOP = 72;
    private static final int MARGIN_BOTTOM = 70;
    private static final int BODY_SIZE = 20;
    private static final int BODY_LEADING = 32;
    private static final int MAX_IMAGE_HEIGHT = 500;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png"
    );

    public PdfRenderResult render(
            RevisionManifest revision,
            String revisionHash,
            PdfImageResolver imageResolver
    ) {
        Objects.requireNonNull(revision, "revision");
        if (revisionHash == null || !SHA_256.matcher(revisionHash).matches()) {
            throw new IllegalArgumentException("Revision hash must be lowercase SHA-256");
        }
        Objects.requireNonNull(imageResolver, "imageResolver");

        String title = title(revision);
        Font bodyFont = chooseFont(visibleText(revision, title), Font.PLAIN, BODY_SIZE);
        Font chapterFont = bodyFont.deriveFont(Font.BOLD, 30f);
        Font titleFont = bodyFont.deriveFont(Font.BOLD, 42f);
        PageComposer pages = new PageComposer(bodyFont, chapterFont, titleFont);
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
                        pages.image(decodeImage(descriptor, imageResolver.resolve(descriptor)));
                        pages.caption(block.text());
                        imageCount++;
                    } else {
                        pages.paragraph(block.text());
                    }
                }
            }
        }
        List<BufferedImage> renderedPages = pages.finish();
        byte[] pdf = PdfWriter.write(renderedPages);
        return new PdfRenderResult(pdf, renderedPages.size(), imageCount, VERSION);
    }

    private static String title(RevisionManifest revision) {
        Object value = revision.novel().extensions().get("title");
        return value instanceof String text && !text.isBlank()
                ? text.strip()
                : "StoryBlock Novel";
    }

    private static String visibleText(RevisionManifest revision, String title) {
        StringBuilder text = new StringBuilder(title);
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            if (chapter.title() != null) {
                text.append(chapter.title());
            }
            for (NarrativeScene scene : chapter.scenes()) {
                if (scene.title() != null) {
                    text.append(scene.title());
                }
                scene.blocks().forEach(block -> text.append(block.text()));
            }
        }
        return text.toString();
    }

    private static Font chooseFont(String text, int style, int size) {
        Set<String> available = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames(Locale.ROOT)
        );
        List<String> candidates = List.of(
                "Noto Serif CJK TC",
                "Noto Sans CJK TC",
                "Droid Sans Fallback",
                "AR PL KaitiM Big5",
                Font.SERIF,
                Font.DIALOG
        );
        Font best = new Font(Font.DIALOG, style, size);
        long bestScore = -1;
        for (String family : candidates) {
            if (!family.equals(Font.SERIF) && !family.equals(Font.DIALOG)
                    && !available.contains(family)) {
                continue;
            }
            Font candidate = new Font(family, style, size);
            long score = text.codePoints().filter(candidate::canDisplay).count();
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
            if (score == text.codePoints().count()) {
                return candidate;
            }
        }
        return best;
    }

    private static BufferedImage decodeImage(BlockImage descriptor, byte[] content) {
        Objects.requireNonNull(content, "Image resolver returned null");
        if (!SUPPORTED_IMAGE_TYPES.contains(descriptor.mediaType())
                || !hash(content).equals(descriptor.contentHash())) {
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

    private static String hash(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return "sha256:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("JVM does not provide SHA-256", failure);
        }
    }

    private static final class PageComposer {
        private final Font bodyFont;
        private final Font chapterFont;
        private final Font titleFont;
        private final List<BufferedImage> pages = new ArrayList<>();
        private BufferedImage page;
        private Graphics2D graphics;
        private int cursorY;

        private PageComposer(Font bodyFont, Font chapterFont, Font titleFont) {
            this.bodyFont = bodyFont;
            this.chapterFont = chapterFont;
            this.titleFont = titleFont;
            newPage();
        }

        private void cover(String title, String revisionId) {
            graphics.setColor(new Color(27, 38, 55));
            graphics.setFont(titleFont);
            List<String> lines = wrap(title, graphics.getFontMetrics(), PAGE_WIDTH - 2 * MARGIN_X);
            int totalHeight = lines.size() * 58;
            int y = Math.max(250, (PAGE_HEIGHT - totalHeight) / 2);
            for (String line : lines) {
                drawCentered(line, y);
                y += 58;
            }
            graphics.setFont(bodyFont.deriveFont(14f));
            graphics.setColor(new Color(100, 108, 119));
            drawCentered("Revision " + revisionId, y + 34);
            newPage();
        }

        private void chapter(String text) {
            ensureSpace(70);
            graphics.setFont(chapterFont);
            graphics.setColor(new Color(27, 38, 55));
            for (String line : wrap(text, graphics.getFontMetrics(), contentWidth())) {
                graphics.drawString(line, MARGIN_X, cursorY + 34);
                cursorY += 42;
            }
            graphics.setColor(new Color(91, 117, 139));
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawLine(MARGIN_X, cursorY + 4, PAGE_WIDTH - MARGIN_X, cursorY + 4);
            cursorY += 28;
        }

        private void scene(String text) {
            ensureSpace(50);
            graphics.setFont(bodyFont.deriveFont(Font.BOLD, 22f));
            graphics.setColor(new Color(70, 79, 91));
            graphics.drawString(text, MARGIN_X, cursorY + 26);
            cursorY += 42;
        }

        private void paragraph(String text) {
            graphics.setFont(bodyFont);
            FontMetrics metrics = graphics.getFontMetrics();
            List<String> lines = wrap("　　" + text, metrics, contentWidth());
            int required = lines.size() * BODY_LEADING + 12;
            ensureSpace(required);
            graphics.setColor(new Color(32, 35, 40));
            for (String line : lines) {
                graphics.drawString(line, MARGIN_X, cursorY + metrics.getAscent());
                cursorY += BODY_LEADING;
            }
            cursorY += 12;
        }

        private void image(BufferedImage source) {
            double scale = Math.min(
                    (double) contentWidth() / source.getWidth(),
                    (double) MAX_IMAGE_HEIGHT / source.getHeight()
            );
            scale = Math.min(1.0d, scale);
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            ensureSpace(height + 24);
            int x = (PAGE_WIDTH - width) / 2;
            graphics.setColor(new Color(225, 228, 232));
            graphics.fillRect(x - 2, cursorY - 2, width + 4, height + 4);
            graphics.drawImage(source, x, cursorY, width, height, Color.WHITE, null);
            cursorY += height + 20;
        }

        private void caption(String text) {
            graphics.setFont(bodyFont.deriveFont(16f));
            FontMetrics metrics = graphics.getFontMetrics();
            List<String> lines = wrap(text, metrics, contentWidth() - 40);
            ensureSpace(lines.size() * 25 + 20);
            graphics.setColor(new Color(84, 88, 96));
            for (String line : lines) {
                int x = (PAGE_WIDTH - metrics.stringWidth(line)) / 2;
                graphics.drawString(line, x, cursorY + metrics.getAscent());
                cursorY += 25;
            }
            cursorY += 20;
        }

        private List<BufferedImage> finish() {
            if (page != null && cursorY <= MARGIN_TOP && pages.size() > 1) {
                pages.removeLast();
                graphics.dispose();
                page = null;
            }
            for (int index = 0; index < pages.size(); index++) {
                Graphics2D footer = pages.get(index).createGraphics();
                configure(footer);
                footer.setFont(bodyFont.deriveFont(13f));
                footer.setColor(new Color(125, 130, 138));
                String number = Integer.toString(index + 1);
                footer.drawString(
                        number,
                        (PAGE_WIDTH - footer.getFontMetrics().stringWidth(number)) / 2,
                        PAGE_HEIGHT - 28
                );
                footer.dispose();
            }
            return List.copyOf(pages);
        }

        private void ensureSpace(int height) {
            if (cursorY + height > PAGE_HEIGHT - MARGIN_BOTTOM) {
                newPage();
            }
        }

        private void newPage() {
            if (graphics != null) {
                graphics.dispose();
            }
            page = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            graphics = page.createGraphics();
            configure(graphics);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            pages.add(page);
            cursorY = MARGIN_TOP;
        }

        private void drawCentered(String text, int baseline) {
            int x = (PAGE_WIDTH - graphics.getFontMetrics().stringWidth(text)) / 2;
            graphics.drawString(text, x, baseline);
        }

        private static void configure(Graphics2D graphics) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
        }

        private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String grapheme : UnicodeText.graphemes(text)) {
                if (!line.isEmpty() && metrics.stringWidth(line + grapheme) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(grapheme);
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines.isEmpty() ? List.of("") : List.copyOf(lines);
        }

        private static int contentWidth() {
            return PAGE_WIDTH - 2 * MARGIN_X;
        }
    }

    private static final class PdfWriter {
        private static final byte[] BINARY_MARKER = {
                '%', (byte) 0xe2, (byte) 0xe3, (byte) 0xcf, (byte) 0xd3, '\n'
        };

        private PdfWriter() {
        }

        private static byte[] write(List<BufferedImage> pages) {
            try {
                List<byte[]> jpegs = pages.stream()
                        .map(PdfWriter::jpeg)
                        .toList();
                int objectCount = 2 + pages.size() * 3;
                long[] offsets = new long[objectCount + 1];
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ascii(output, "%PDF-1.4\n");
                output.write(BINARY_MARKER);
                object(output, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");

                StringBuilder kids = new StringBuilder("[");
                for (int index = 0; index < pages.size(); index++) {
                    kids.append(3 + index * 3).append(" 0 R ");
                }
                kids.append(']');
                object(
                        output,
                        offsets,
                        2,
                        "<< /Type /Pages /Count " + pages.size() + " /Kids " + kids + " >>"
                );

                for (int index = 0; index < pages.size(); index++) {
                    int pageObject = 3 + index * 3;
                    int imageObject = pageObject + 1;
                    int contentObject = pageObject + 2;
                    object(
                            output,
                            offsets,
                            pageObject,
                            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                                    + "/Resources << /XObject << /Im0 " + imageObject
                                    + " 0 R >> >> /Contents " + contentObject + " 0 R >>"
                    );
                    streamObject(
                            output,
                            offsets,
                            imageObject,
                            "<< /Type /XObject /Subtype /Image /Width " + PAGE_WIDTH
                                    + " /Height " + PAGE_HEIGHT
                                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 "
                                    + "/Filter /DCTDecode /Length " + jpegs.get(index).length
                                    + " >>",
                            jpegs.get(index)
                    );
                    byte[] commands = (
                            "q\n595 0 0 842 0 0 cm\n/Im0 Do\nQ\n"
                    ).getBytes(StandardCharsets.US_ASCII);
                    streamObject(
                            output,
                            offsets,
                            contentObject,
                            "<< /Length " + commands.length + " >>",
                            commands
                    );
                }

                long xref = output.size();
                ascii(output, "xref\n0 " + (objectCount + 1) + "\n");
                ascii(output, "0000000000 65535 f \n");
                for (int object = 1; object <= objectCount; object++) {
                    ascii(output, String.format(
                            Locale.ROOT, "%010d 00000 n \n", offsets[object]
                    ));
                }
                ascii(
                        output,
                        "trailer\n<< /Size " + (objectCount + 1)
                                + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"
                );
                return output.toByteArray();
            } catch (IOException failure) {
                throw new IllegalStateException("PDF encoding failed", failure);
            }
        }

        private static byte[] jpeg(BufferedImage page) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                throw new IllegalStateException("JVM does not provide a JPEG writer");
            }
            ImageWriter writer = writers.next();
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(output);
                JPEGImageWriteParam parameters = new JPEGImageWriteParam(Locale.ROOT);
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.86f);
                parameters.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
                parameters.setOptimizeHuffmanTables(false);
                writer.write(null, new IIOImage(page, null, null), parameters);
                output.flush();
                return bytes.toByteArray();
            } catch (IOException failure) {
                throw new IllegalStateException("PDF page JPEG encoding failed", failure);
            } finally {
                writer.dispose();
            }
        }

        private static void object(
                ByteArrayOutputStream output,
                long[] offsets,
                int number,
                String body
        ) throws IOException {
            offsets[number] = output.size();
            ascii(output, number + " 0 obj\n" + body + "\nendobj\n");
        }

        private static void streamObject(
                ByteArrayOutputStream output,
                long[] offsets,
                int number,
                String dictionary,
                byte[] content
        ) throws IOException {
            offsets[number] = output.size();
            ascii(output, number + " 0 obj\n" + dictionary + "\nstream\n");
            output.write(content);
            ascii(output, "\nendstream\nendobj\n");
        }

        private static void ascii(ByteArrayOutputStream output, String value)
                throws IOException {
            output.write(value.getBytes(StandardCharsets.US_ASCII));
        }
    }
}

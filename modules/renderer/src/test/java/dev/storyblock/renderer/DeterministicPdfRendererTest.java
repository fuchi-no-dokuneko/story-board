package dev.storyblock.renderer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DeterministicPdfRendererTest {
    private static final String REVISION_HASH = "sha256:" + "a".repeat(64);

    @Test
    void sameRevisionAndImageProduceByteIdenticalValidPdf() throws IOException {
        byte[] png = png();
        RevisionManifest revision = revision(png);
        DeterministicPdfRenderer renderer = new DeterministicPdfRenderer();

        PdfRenderResult first = renderer.render(revision, REVISION_HASH, ignored -> png);
        PdfRenderResult second = renderer.render(revision, REVISION_HASH, ignored -> png);

        assertArrayEquals(first.content(), second.content());
        assertTrue(new String(
                first.content(), 0, 8, StandardCharsets.US_ASCII
        ).startsWith("%PDF-1.4"));
        assertTrue(new String(
                first.content(),
                first.content().length - 7,
                7,
                StandardCharsets.US_ASCII
        ).contains("%%EOF"));
        assertTrue(first.pageCount() >= 2);
        assertEquals(1, first.imageCount());
        assertEquals(DeterministicPdfRenderer.VERSION, first.rendererVersion());
    }

    @Test
    void resolverBytesMustMatchDescriptorHash() throws IOException {
        byte[] png = png();
        RevisionManifest revision = revision(png);

        assertThrows(
                IllegalArgumentException.class,
                () -> new DeterministicPdfRenderer().render(
                        revision, REVISION_HASH, ignored -> new byte[] {1, 2, 3}
                )
        );
    }

    private static RevisionManifest revision(byte[] png) {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        Ids.ArtifactId artifactId = Ids.ArtifactId.create();
        BlockImage image = new BlockImage(
                artifactId,
                CanonicalJson.hashBytes(png),
                "image/png",
                120,
                80,
                "白色背景上的角色立繪。"
        );
        NarrativeBlock text = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(0, 2),
                "逆潮越過港牆，燈塔仍在霧裏發亮。",
                BlockMetadata.empty(),
                Map.of()
        );
        NarrativeBlock illustration = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(1, 2),
                "岑霧握着黃銅鑰匙站在白色背景前。",
                BlockMetadata.empty(),
                image.attachTo(Map.of())
        );
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "潮門",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                List.of(text, illustration),
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId, OrderKey.initial(), "第一章　逆潮", List.of(scene), Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-29T00:00:00Z"),
                new NarrativeNovel(
                        Ids.NovelId.create(), List.of(chapter), Map.of("title", "逆潮燈塔")
                )
        );
    }

    private static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 120, 80);
        graphics.setColor(new Color(25, 80, 120));
        graphics.fillOval(35, 10, 50, 60);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }
}

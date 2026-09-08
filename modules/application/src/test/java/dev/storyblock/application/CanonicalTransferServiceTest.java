package dev.storyblock.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CanonicalTransferServiceTest {
    private static final Instant CREATED_AT = Instant.parse("2026-08-29T00:00:00Z");

    @Test
    void replayBindsEveryImageDescriptorToDecodedPortableBytes() throws IOException {
        byte[] png = png();
        Fixture fixture = fixture(png, 120);

        assertDoesNotThrow(() -> CanonicalTransferService.verifyReplay(fixture.document()));
        assertThrows(
                CanonicalPackageException.class,
                () -> CanonicalTransferService.verifyReplay(
                        CanonicalNovelPackage.genesis(fixture.revision())
                )
        );

        Fixture wrongDimensions = fixture(png, 121);
        assertThrows(
                CanonicalPackageException.class,
                () -> CanonicalTransferService.verifyReplay(wrongDimensions.document())
        );
    }

    private static Fixture fixture(byte[] png, int descriptorWidth) {
        Ids.NovelId novelId = Ids.NovelId.create();
        Ids.RevisionId revisionId = Ids.RevisionId.create();
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        Ids.ArtifactId artifactId = Ids.ArtifactId.create();
        String hash = CanonicalJson.hashBytes(png);
        BlockImage image = new BlockImage(
                artifactId,
                hash,
                "image/png",
                descriptorWidth,
                80,
                "白色背景上的藍色信號。"
        );
        NarrativeBlock block = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.initial(),
                "藍色信號顯現在白色背景上。",
                BlockMetadata.empty(),
                image.attachTo(Map.of())
        );
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "信號",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                List.of(block),
                Map.of()
        );
        RevisionManifest manifest = new RevisionManifest(
                revisionId,
                null,
                CREATED_AT,
                new NarrativeNovel(
                        novelId,
                        List.of(new NarrativeChapter(
                                chapterId,
                                OrderKey.initial(),
                                "第一章",
                                List.of(scene),
                                Map.of()
                        )),
                        Map.of("title", "圖像測試")
                )
        );
        var canonical = NarrativeCanonicalMapper.toCanonical(manifest);
        var artifact = new CanonicalNovelPackage.ArtifactEntry(
                artifactId,
                revisionId,
                "narrative-image",
                "image/png",
                "identity",
                hash,
                png,
                CREATED_AT
        );
        return new Fixture(
                canonical,
                CanonicalNovelPackage.assemble(
                        List.of(new CanonicalNovelPackage.RevisionEntry(0, canonical)),
                        List.of(),
                        List.of(artifact)
                )
        );
    }

    private static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 120, 80);
        graphics.setColor(Color.BLUE);
        graphics.fillOval(35, 10, 50, 60);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private record Fixture(
            dev.storyblock.contracts.CanonicalRevision revision,
            CanonicalNovelPackage document
    ) {
    }
}

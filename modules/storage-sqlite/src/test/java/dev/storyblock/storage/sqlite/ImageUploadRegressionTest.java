package dev.storyblock.storage.sqlite;

import dev.storyblock.application.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ImageUploadRegressionTest {
    @TempDir Path directory;
    @Test void damagedPixelsFailAndLostUploadResponsesReplayAfterHeadAdvances() throws Exception {
        var base = RevisionStoreTestFixture.genesis();
        byte[] png = png(0x77aacc);
        try (var store = SqliteRevisionStore.open(directory.resolve("images.db"))) {
            store.createNovel(base,RevisionStoreTestFixture.hash(base)); var service = new ImageUploadService(store);
            String hash = RevisionStoreTestFixture.hash(base);
            assertThrows(IllegalArgumentException.class,() -> service.upload(base.novel().id(),hash,"truncated",
                    Arrays.copyOf(png,33),base.createdAt()));
            var original = service.upload(base.novel().id(),hash,"lost-response",png,base.createdAt());
            new CommitService(store).commit(RevisionStoreTestFixture.replace(base,"later-edit","第二句。"),
                    Ids.RevisionId.create(),base.createdAt().plusSeconds(1));
            var retry = service.upload(base.novel().id(),hash,"lost-response",png,base.createdAt().plusSeconds(2));
            assertTrue(retry.idempotentReplay());
            assertEquals(original.artifact().artifactId(),retry.artifact().artifactId());
            assertEquals(base.createdAt(),retry.artifact().createdAt());
            assertArrayEquals(png,retry.artifact().content());
            assertThrows(IdempotencyConflictException.class,() -> service.upload(base.novel().id(),hash,
                    "lost-response",png(0xff3300),base.createdAt().plusSeconds(3)));
            assertThrows(StaleHeadException.class,() -> service.upload(base.novel().id(),hash,
                    "new-upload",png,base.createdAt().plusSeconds(3)));
            assertThrows(IdempotencyConflictException.class,() -> service.upload(base.novel().id(),store.getHead(base.novel().id()).contentHash(),
                    "lost-response",png,base.createdAt().plusSeconds(3)));
        }
    }
    static byte[] png(int color) throws Exception {
        var image = new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB); image.setRGB(0,0,color);
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image,"png",bytes); return bytes.toByteArray();
    }
}

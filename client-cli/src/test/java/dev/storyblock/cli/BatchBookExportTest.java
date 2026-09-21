package dev.storyblock.cli;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BatchBookExportTest {
    @Test void batchExportsProduceOfflineBooksPdfAndEscapedIndex() throws Exception {
        Path root = Files.createDirectories(Path.of("target/batch-export-"+UUID.randomUUID()));
        Path database = root.resolve("books.db"), output = root.resolve("output");
        var picture = new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var imageBytes = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(picture,"png",imageBytes);
        byte[] png = imageBytes.toByteArray();
        var imageId = dev.storyblock.domain.Ids.ArtifactId.create();
        String imageHash = dev.storyblock.contracts.CanonicalJson.hashBytes(png);
        var image = new dev.storyblock.domain.BlockImage(imageId,imageHash,"image/png",2,2,"雨點");
        var first = MaintenanceFixture.book("春雨 <第一冊>",image.attachTo(Map.of()));
        var second = MaintenanceFixture.book("第二冊",Map.of());
        try (var store = SqliteRevisionStore.open(database)) {
            for (var book : List.of(first,second)) store.createNovel(book,NarrativeCanonicalMapper.toCanonical(book).contentHash());
            store.putPortableArtifact(new dev.storyblock.storage.PortableArtifactPutRequest(store.getHead(first.novel().id()),"image",
                    new dev.storyblock.storage.StoredArtifact(imageId,first.novel().id(),first.id(),"narrative-image","image/png","identity",
                            imageHash,png,first.createdAt(),true)));
        }
        var errors = new ByteArrayOutputStream();
        assertEquals(0,StoryBlockCli.run(new String[]{"export-books",database.toString(),output.toString(),"both"},
                System.out,new PrintStream(errors)),errors.toString());
        assertTrue(Files.readString(output.resolve("index.html")).contains("春雨 &lt;第一冊&gt;"));
        assertTrue(Files.readString(output.resolve(first.novel().id().value()+".html")).contains("data:image/png;base64,"+Base64.getEncoder().encodeToString(png)));
        for (var book : List.of(first,second)) {
            String id = book.novel().id().value();
            assertTrue(Files.readString(output.resolve(id+".html")).contains("雨水落在窗邊。"));
            byte[] pdf = Files.readAllBytes(output.resolve(id+".pdf"));
            assertEquals("%PDF",new String(pdf,0,4,java.nio.charset.StandardCharsets.US_ASCII));
        }
        assertEquals(1,StoryBlockCli.run(new String[]{"export-books",root.resolve("missing.db").toString(),output.toString(),"both"},System.out,System.err));
    }
}

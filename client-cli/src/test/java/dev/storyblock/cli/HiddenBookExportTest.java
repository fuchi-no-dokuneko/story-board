package dev.storyblock.cli;

import dev.storyblock.domain.*;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HiddenBookExportTest {
    @TempDir Path root;
    @Test void offlineBookAndBatchExportsExcludeNotesAndKeepCanonicalSource() throws Exception {
        var base = MaintenanceFixture.book("Teacup",Map.of());
        var chapter = base.novel().chapters().getFirst(); var scene = chapter.scenes().getFirst();
        String sentinel = "HIDDEN_SENTINEL_foreshadowing\nPayoff in chapter 9. "+"symbolism ".repeat(20);
        var note = NarrativeBlock.create(Ids.BlockId.create(),OrderKey.between(scene.blocks().getFirst().orderKey(),null),
                sentinel,BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true));
        var full = new RevisionManifest(base.id(),null,base.createdAt(),new NarrativeNovel(base.novel().id(),
                List.of(chapter.withScenes(List.of(scene.withBlocks(List.of(scene.blocks().getFirst(),note))))),base.novel().extensions()));
        Path database = root.resolve("books.db");
        try (var store = SqliteRevisionStore.open(database)) {
            store.createNovel(full,NarrativeCanonicalMapper.toCanonical(full).contentHash());
            assertEquals(HtmlBook.render(base,store),HtmlBook.render(full,store));
            assertFalse(HtmlBook.render(full,store).contains("HIDDEN_SENTINEL"));
        }
        Path output = root.resolve("export");
        assertEquals(0,BatchBookExport.run(new String[]{"export-books",database.toString(),output.toString(),"both"},System.out,System.err));
        assertFalse(Files.readString(output.resolve(base.novel().id().value()+".html")).contains("HIDDEN_SENTINEL"));
        var renderer = new dev.storyblock.renderer.DeterministicPdfRenderer();
        assertArrayEquals(renderer.render(base,NarrativeCanonicalMapper.toCanonical(base).contentHash(),ignored -> null).content(),
                Files.readAllBytes(output.resolve(base.novel().id().value()+".pdf")));
        try (var store = SqliteRevisionStore.open(database)) {
            assertEquals(full,store.getRevision(full.novel().id(),full.id()).manifest());
        }
    }
}

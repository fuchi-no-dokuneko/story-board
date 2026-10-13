package dev.storyblock.api.http;

import dev.storyblock.api.quality.*;
import dev.storyblock.application.*;
import dev.storyblock.contracts.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HiddenQualityTransferTest {
    @TempDir Path root;
    @Test void qualityEvidenceAndSpansEqualNoteRemovalWhileTransferPreservesNotes() throws Exception {
        var full = HiddenHttpFixture.revision(); var plain = HiddenHttpFixture.withoutHidden(full);
        try (var source = SqliteRevisionStore.open(root.resolve("source.db"));
             var clean = SqliteRevisionStore.open(root.resolve("clean.db"));
             var target = SqliteRevisionStore.open(root.resolve("target.db"))) {
            source.createNovel(full,CacheRevisionFixture.hash(full));
            clean.createNovel(plain,CacheRevisionFixture.hash(plain));
            var reference = new QualityReference(root.resolve("absent.json").toString());
            var fullResult = new NovelQualityService(new NovelReadCache(source),reference).analyze(
                    full.novel().id().value(),full.id().value(),CacheRevisionFixture.hash(full),Set.of());
            var plainResult = new NovelQualityService(new NovelReadCache(clean),reference).analyze(
                    plain.novel().id().value(),plain.id().value(),CacheRevisionFixture.hash(plain),Set.of());
            assertEquals(plainResult.get("block_spans"),fullResult.get("block_spans"));
            assertEquals(CanonicalJson.string(plainResult.get("quality_report")),
                    CanonicalJson.string(fullResult.get("quality_report")));
            assertFalse(CanonicalJson.string(fullResult).contains(HiddenHttpFixture.SENTINEL));
            var bytes = new CanonicalTransferService(source).exportPackage(full.novel().id());
            assertTrue(new String(bytes,java.nio.charset.StandardCharsets.UTF_8).contains(HiddenHttpFixture.SENTINEL));
            new CanonicalTransferService(target).importDocument(CanonicalExportFormat.PACKAGE,bytes,"hidden-transfer",full.createdAt());
            assertEquals(full,target.getRevision(full.novel().id(),full.id()).manifest());
            assertArrayEquals(bytes,new CanonicalTransferService(target).exportPackage(full.novel().id()));
        }
    }
}

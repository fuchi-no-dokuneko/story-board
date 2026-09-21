package dev.storyblock.cli;

import dev.storyblock.application.*;
import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceScriptsTest {
    @Test void deletionPreviewIsReadOnlyAndApplyKeepsOtherBooksAndTriggers() throws Exception {
        Path directory = Files.createDirectories(Path.of("target/maintenance-"+UUID.randomUUID()));
        Path database = directory.resolve("books.db").toAbsolutePath();
        var first = MaintenanceFixture.book("刪除 <春雨>",Map.of());
        var other = MaintenanceFixture.book("保留的小說",Map.of());
        try (var store = SqliteRevisionStore.open(database)) {
            for (var book : List.of(first,other)) {
                store.createNovel(book,NarrativeCanonicalMapper.toCanonical(book).contentHash());
                var h = store.getHead(book.novel().id());
                new CanonicalTransferService(store).requestExport(book.novel().id(),book.id(),h.contentHash(),CanonicalExportFormat.REVISION,"export",book.createdAt());
            }
        }
        String preview = script(database,"--novel",first.novel().id().value());
        assertTrue(preview.contains("刪除 <春雨>")); assertTrue(preview.contains("Preview only"));
        try (var store = SqliteRevisionStore.open(database)) { assertEquals(2,store.listNovels().size()); }
        String result = script(database,"--novel",first.novel().id().value(),"--apply");
        assertTrue(result.contains("Deleted / 已刪除: 1"));
        try (var store = SqliteRevisionStore.open(database)) {
            assertEquals(List.of(other.novel().id()),store.listNovels());
            assertTrue(new ReplayService(store).verifyAllHeads().valid());
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:"+database); var sql = connection.createStatement()) {
            assertThrows(SQLException.class,() -> sql.executeUpdate("DELETE FROM revisions"));
            try (var rows = sql.executeQuery("PRAGMA foreign_key_check")) { assertFalse(rows.next()); }
            try (var rows = sql.executeQuery("SELECT COUNT(*) FROM export_jobs")) { rows.next(); assertEquals(1,rows.getInt(1)); }
        }
    }
    private static String script(Path database, String... args) throws Exception {
        var command = new ArrayList<>(List.of("python3","-B","../server/scripts/delete_novels.py","--database",database.toString()));
        command.addAll(List.of(args));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0,process.waitFor(),output); return output;
    }
}

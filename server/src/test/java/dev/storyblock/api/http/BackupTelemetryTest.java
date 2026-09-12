package dev.storyblock.api.http;

import static org.junit.jupiter.api.Assertions.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupTelemetryTest {
    @TempDir Path directory;

    @Test
    void backupAgeUsesPublishedCompressedArchivesAndIgnoresPendingManifests() throws Exception {
        Instant now = Instant.parse("2026-09-08T12:00:00Z");
        Path backups = Files.createDirectory(directory.resolve("backups"));
        var registry = new SimpleMeterRegistry();
        try (var store = SqliteRevisionStore.open(directory.resolve("storyblock.db"))) {
            new StoryBlockTelemetry(store, registry, Clock.fixed(now, ZoneOffset.UTC), backups.toString());
            var age = registry.get("backup_age_seconds").gauge();
            assertTrue(Double.isNaN(age.value()));
            manifest(backups, "storyblock-20260908T115500Z.db.zst.json", now.minusSeconds(300));
            assertEquals(300.0, age.value());
            manifest(backups, "storyblock-20260908T115900Z.db.zst.json", now.minusSeconds(60));
            manifest(backups, "storyblock-20260908T120000Z.db.zst.json.pending", now);
            assertEquals(60.0, age.value());
        } finally { registry.close(); }
    }

    private static void manifest(Path directory, String name, Instant time) throws Exception {
        Path file = Files.writeString(directory.resolve(name), "{}");
        Files.setLastModifiedTime(file, FileTime.from(time));
    }
}

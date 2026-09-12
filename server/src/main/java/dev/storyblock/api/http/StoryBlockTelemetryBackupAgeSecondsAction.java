package dev.storyblock.api.http;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

final class StoryBlockTelemetryBackupAgeSecondsAction {
    static double backupAgeSeconds(StoryBlockTelemetry self)  {
        if (self.backupManifestDirectory == null || !Files.isDirectory(self.backupManifestDirectory)) {
            return Double.NaN;
        }
        try (var files = Files.list(self.backupManifestDirectory)) {
            Instant newest = files
                    .filter(path -> path.getFileName().toString().endsWith(".db.zst.json"))
                    .map(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant();
                        } catch (IOException exception) {
                            return Instant.EPOCH;
                        }
                    })
                    .max(Instant::compareTo)
                    .orElse(null);
            return newest == null ? Double.NaN
                    : Math.max(0L, Duration.between(newest, Instant.now(self.clock)).toSeconds());
        } catch (IOException exception) {
            return Double.NaN;
        }
    }
}

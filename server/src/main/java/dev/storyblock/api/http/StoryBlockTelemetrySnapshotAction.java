package dev.storyblock.api.http;

import dev.storyblock.storage.sqlite.SqliteOperationalSnapshot;
import java.time.Instant;
import static dev.storyblock.api.http.StoryBlockTelemetry.CachedSnapshot;

final class StoryBlockTelemetrySnapshotAction {
    static SqliteOperationalSnapshot snapshot(StoryBlockTelemetry self)  {
        long now = System.nanoTime();
        CachedSnapshot current = self.cached;
        if (current == null || now - current.loadedAtNanos() >= StoryBlockTelemetry.CACHE_NANOS) {
            synchronized (self) {
                current = self.cached;
                if (current == null || now - current.loadedAtNanos() >= StoryBlockTelemetry.CACHE_NANOS) {
                    current = new CachedSnapshot(
                            self.store.operationalSnapshot(Instant.now(self.clock)), now
                    );
                    self.cached = current;
                }
            }
        }
        return current.value();
    }
}

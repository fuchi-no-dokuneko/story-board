package dev.storyblock.api.http;

import dev.storyblock.storage.sqlite.SqliteOperationalSnapshot;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

final class OperationalHealthConfigurationWalHealthAction {
    static HealthIndicator walHealth(OperationalHealthConfiguration self, SqliteRevisionStore store, long maximumWalBytes)  {
        return () -> {
            SqliteOperationalSnapshot snapshot = store.operationalSnapshot(
                    java.time.Instant.now()
            );
            var checkpoint = store.checkpointPassive();
            Health.Builder health = snapshot.walBytes() <= maximumWalBytes
                    && checkpoint.busy() == 0 ? Health.up() : Health.down();
            return health
                    .withDetail("bytes", snapshot.walBytes())
                    .withDetail("checkpoint_ms", checkpoint.durationMillis())
                    .withDetail("checkpoint_busy", checkpoint.busy())
                    .build();
        };
    }
}

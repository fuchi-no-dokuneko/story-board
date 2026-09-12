package dev.storyblock.api.http;

import dev.storyblock.storage.sqlite.SqliteOperationalSnapshot;
import java.time.Duration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

final class OperationalHealthConfigurationWorkerQueueHealthAction {
    static HealthIndicator workerQueueHealth(OperationalHealthConfiguration self, StoryBlockTelemetry telemetry, Duration maximumAge)  {
        return () -> {
            SqliteOperationalSnapshot snapshot = telemetry.snapshot();
            Health.Builder health = snapshot.oldestJobAgeSeconds() <= maximumAge.toSeconds()
                    ? Health.up() : Health.down();
            return health
                    .withDetail("queue_depth", snapshot.queueDepth())
                    .withDetail("oldest_job_age", snapshot.oldestJobAgeSeconds())
                    .build();
        };
    }
}

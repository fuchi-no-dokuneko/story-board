package dev.storyblock.api.http;

import java.time.Duration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

final class OperationalHealthConfigurationBackupHealthAction {
    static HealthIndicator backupHealth(OperationalHealthConfiguration self, StoryBlockTelemetry telemetry, Duration maximumAge)  {
        return () -> {
            double age = telemetry.backupAgeSeconds();
            if (Double.isNaN(age)) {
                return Health.unknown().withDetail("configured", false).build();
            }
            Health.Builder health = age <= maximumAge.toSeconds()
                    ? Health.up() : Health.down();
            return health.withDetail("age_seconds", (long) age).build();
        };
    }
}

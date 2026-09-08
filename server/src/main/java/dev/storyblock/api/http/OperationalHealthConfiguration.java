package dev.storyblock.api.http;

import dev.storyblock.storage.sqlite.SqliteOperationalSnapshot;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OperationalHealthConfiguration {
    @Bean("sqlite")
    HealthIndicator sqliteHealth(SqliteRevisionStore store) {
        return () -> {
            try {
                store.verifyReadableAndWritable();
                return Health.up().build();
            } catch (RuntimeException failure) {
                return Health.down(failure).build();
            }
        };
    }

    @Bean("migration")
    HealthIndicator migrationHealth(StoryBlockTelemetry telemetry) {
        return () -> Health.up()
                .withDetail("version", telemetry.snapshot().migrationVersion())
                .build();
    }

    @Bean("wal")
    HealthIndicator walHealth(
            SqliteRevisionStore store,
            @Value("${storyblock.health.max-wal-bytes:67108864}") long maximumWalBytes
    ) {
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

    @Bean("backup")
    HealthIndicator backupHealth(
            StoryBlockTelemetry telemetry,
            @Value("${storyblock.backup.max-age:PT2H}") Duration maximumAge
    ) {
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

    @Bean("workerQueue")
    HealthIndicator workerQueueHealth(
            StoryBlockTelemetry telemetry,
            @Value("${storyblock.health.max-job-age:PT15M}") Duration maximumAge
    ) {
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

    @Bean("artifactStorage")
    HealthIndicator artifactStorageHealth(StoryBlockTelemetry telemetry) {
        return () -> Health.up()
                .withDetail("artifact_bytes", telemetry.snapshot().artifactBytes())
                .build();
    }
}

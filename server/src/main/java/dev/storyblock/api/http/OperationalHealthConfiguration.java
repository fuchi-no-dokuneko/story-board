package dev.storyblock.api.http;

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
        return OperationalHealthConfigurationWalHealthAction.walHealth(this, store, maximumWalBytes);
    }

    @Bean("backup")
    HealthIndicator backupHealth(
            StoryBlockTelemetry telemetry,
            @Value("${storyblock.backup.max-age:PT2H}") Duration maximumAge
    ) {
        return OperationalHealthConfigurationBackupHealthAction.backupHealth(this, telemetry, maximumAge);
    }

    @Bean("workerQueue")
    HealthIndicator workerQueueHealth(
            StoryBlockTelemetry telemetry,
            @Value("${storyblock.health.max-job-age:PT15M}") Duration maximumAge
    ) {
        return OperationalHealthConfigurationWorkerQueueHealthAction.workerQueueHealth(this, telemetry, maximumAge);
    }

    @Bean("artifactStorage")
    HealthIndicator artifactStorageHealth(StoryBlockTelemetry telemetry) {
        return () -> Health.up()
                .withDetail("artifact_bytes", telemetry.snapshot().artifactBytes())
                .build();
    }
}

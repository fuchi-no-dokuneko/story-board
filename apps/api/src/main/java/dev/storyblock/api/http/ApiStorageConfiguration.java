package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.CommitService;
import dev.storyblock.application.DetectorService;
import dev.storyblock.application.MonitorService;
import dev.storyblock.application.RenderService;
import dev.storyblock.security.AccessKeyService;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiStorageConfiguration {
    @Bean(destroyMethod = "close")
    SqliteRevisionStore revisionStore(
            @Value("${storyblock.database.path:data/storyblock.db}") String databasePath
    ) throws IOException {
        if (databasePath.isBlank()) {
            throw new IllegalArgumentException("storyblock.database.path cannot be blank");
        }
        return SqliteRevisionStore.open(Path.of(databasePath));
    }

    @Bean
    CanonicalTransferService canonicalTransferService(SqliteRevisionStore store) {
        return new CanonicalTransferService(store);
    }

    @Bean
    CommitService commitService(SqliteRevisionStore store) {
        return new CommitService(store);
    }

    @Bean
    RenderService renderService(SqliteRevisionStore store) {
        return new RenderService(store);
    }

    @Bean
    DetectorService detectorService(SqliteRevisionStore store) {
        return new DetectorService(store);
    }

    @Bean
    MonitorService monitorService(SqliteRevisionStore store) {
        return new MonitorService(store, store);
    }

    @Bean
    AccessKeyService accessKeyService(
            SqliteRevisionStore store,
            @Value("${storyblock.security.pepper:}") String pepper
    ) {
        return new AccessKeyService(store, pepper.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    Clock apiClock() {
        return Clock.systemUTC();
    }
}

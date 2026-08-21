package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.io.IOException;
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
    Clock apiClock() {
        return Clock.systemUTC();
    }
}

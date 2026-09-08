package dev.storyblock.api.http;

import dev.storyblock.application.*;
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
  NovelCatalogService novelCatalogService(SqliteRevisionStore store) {
    return new NovelCatalogService(store);
  }

  @Bean
  AgentNovelRegistrationService agentNovelRegistrationService(
      CanonicalTransferService transfers,
      NovelCatalogService catalog
  ) {
    return new AgentNovelRegistrationService(transfers, catalog);
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
  ImageUploadService imageUploadService(SqliteRevisionStore store) {
    return new ImageUploadService(store);
  }

  @Bean
  PdfRenderService pdfRenderService(SqliteRevisionStore store) {
    return new PdfRenderService(store);
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
  StyleProfileService styleProfileService(SqliteRevisionStore store) {
    return new StyleProfileService(store);
  }

  @Bean
  StyleAnalysisService styleAnalysisService(SqliteRevisionStore store) {
    return new StyleAnalysisService(store, store, store);
  }

  @Bean
  RewriteGateService rewriteGateService(SqliteRevisionStore store) {
    return new RewriteGateService(store, store, store);
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

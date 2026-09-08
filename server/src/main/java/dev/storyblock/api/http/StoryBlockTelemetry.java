package dev.storyblock.api.http;

import dev.storyblock.detector.DetectorFinding;
import dev.storyblock.detector.FindingCode;
import dev.storyblock.storage.sqlite.SqliteOperationalSnapshot;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class StoryBlockTelemetry {
  static final long CACHE_NANOS = Duration.ofSeconds(1).toNanos();

  final SqliteRevisionStore store;
  final MeterRegistry registry;
  final Clock clock;
  final Path backupManifestDirectory;
  final Map<FindingCode, Counter> detectorCounters = new EnumMap<>(FindingCode.class);
  final Map<String, Counter> denialCounters = new ConcurrentHashMap<>();
  volatile CachedSnapshot cached;

  StoryBlockTelemetry(
      SqliteRevisionStore store,
      MeterRegistry registry,
      Clock clock,
      @Value("${storyblock.backup.manifest-directory:}") String backupDirectory
  ) {
    this.store = store;
    this.registry = registry;
    this.clock = clock;
    this.backupManifestDirectory = backupDirectory.isBlank()
        ? null : Path.of(backupDirectory).toAbsolutePath().normalize();

    TelemetryMeters.register(this);
  }

  void recordDetectorFindings(List<DetectorFinding> findings) {
    findings.forEach(finding -> detectorCounters.get(finding.code()).increment());
  }

  void recordAuthDenied(String reason) {
    denialCounters.computeIfAbsent(reason, value -> Counter.builder("auth_denied_total")
        .tag("reason", value)
        .register(registry)).increment();
  }

  SqliteOperationalSnapshot snapshot() {
    return StoryBlockTelemetrySnapshotAction.snapshot(this);
  }

  double backupAgeSeconds() {
    return StoryBlockTelemetryBackupAgeSecondsAction.backupAgeSeconds(this);
  }

  void gauge(
      String name,
      ToDoubleFunction<SqliteOperationalSnapshot> measurement
  ) {
    Gauge.builder(name, this, telemetry -> measurement.applyAsDouble(telemetry.snapshot()))
        .register(registry);
  }

  record CachedSnapshot(SqliteOperationalSnapshot value, long loadedAtNanos) {
  }
}

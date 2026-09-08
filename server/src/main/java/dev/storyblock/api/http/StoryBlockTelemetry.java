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

    gauge("commit_wait_ms", SqliteOperationalSnapshot::commitWaitMillis);
    gauge("commit_transaction_ms", SqliteOperationalSnapshot::commitTransactionMillis);
    gauge("sqlite_busy_total", SqliteOperationalSnapshot::sqliteBusyTotal);
    gauge("wal_bytes", SqliteOperationalSnapshot::walBytes);
    gauge("checkpoint_ms", SqliteOperationalSnapshot::checkpointMillis);
    gauge("queue_depth", SqliteOperationalSnapshot::queueDepth);
    gauge("oldest_job_age", SqliteOperationalSnapshot::oldestJobAgeSeconds);
    gauge("analysis_duration_ms", SqliteOperationalSnapshot::analysisDurationMillis);
    gauge("rewrite_duration_ms", SqliteOperationalSnapshot::rewriteDurationMillis);
    gauge("stale_proposal_total", SqliteOperationalSnapshot::staleProposalTotal);
    gauge("artifact_bytes", SqliteOperationalSnapshot::artifactBytes);
    Gauge.builder("backup_age_seconds", this, StoryBlockTelemetry::backupAgeSeconds)
        .description("Age of the newest encrypted backup manifest")
        .baseUnit("seconds")
        .register(registry);
    for (FindingCode code : FindingCode.values()) {
      detectorCounters.put(code, Counter.builder("detector_findings_total")
          .tag("code", code.name())
          .register(registry));
    }
    for (String reason : List.of("missing", "invalid", "scope", "novel")) {
      denialCounters.put(reason, Counter.builder("auth_denied_total")
          .tag("reason", reason)
          .register(registry));
    }
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

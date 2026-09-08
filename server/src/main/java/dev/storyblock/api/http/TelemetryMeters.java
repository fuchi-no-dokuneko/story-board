package dev.storyblock.api.http;

import dev.storyblock.detector.FindingCode;
import dev.storyblock.storage.sqlite.SqliteOperationalSnapshot;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import java.util.List;

final class TelemetryMeters {
  static void register(StoryBlockTelemetry self) {
    self.gauge("commit_wait_ms", SqliteOperationalSnapshot::commitWaitMillis);
    self.gauge("commit_transaction_ms", SqliteOperationalSnapshot::commitTransactionMillis);
    self.gauge("sqlite_busy_total", SqliteOperationalSnapshot::sqliteBusyTotal);
    self.gauge("wal_bytes", SqliteOperationalSnapshot::walBytes);
    self.gauge("checkpoint_ms", SqliteOperationalSnapshot::checkpointMillis);
    self.gauge("queue_depth", SqliteOperationalSnapshot::queueDepth);
    self.gauge("oldest_job_age", SqliteOperationalSnapshot::oldestJobAgeSeconds);
    self.gauge("analysis_duration_ms", SqliteOperationalSnapshot::analysisDurationMillis);
    self.gauge("rewrite_duration_ms", SqliteOperationalSnapshot::rewriteDurationMillis);
    self.gauge("stale_proposal_total", SqliteOperationalSnapshot::staleProposalTotal);
    self.gauge("artifact_bytes", SqliteOperationalSnapshot::artifactBytes);
    Gauge.builder("backup_age_seconds", self, StoryBlockTelemetry::backupAgeSeconds)
        .description("Age of the newest encrypted backup manifest")
        .baseUnit("seconds")
        .register(self.registry);
    for (FindingCode code : FindingCode.values()) {
      self.detectorCounters.put(code, Counter.builder("detector_findings_total")
          .tag("code", code.name())
          .register(self.registry));
    }
    for (String reason : List.of("missing", "invalid", "scope", "novel")) {
      self.denialCounters.put(reason, Counter.builder("auth_denied_total")
          .tag("reason", reason)
          .register(self.registry));
    }  }
}

package dev.storyblock.monitor;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record StoredMonitorRun(
    Ids.MonitorRunId runId,
    Ids.MonitorOutputId outputId,
    Ids.NovelId novelId,
    Ids.RevisionId revisionId,
    String revisionHash,
    Ids.BlockId targetBlockId,
    int neighborCount,
    String monitorVersion,
    String ruleVersion,
    List<MonitorBlockFingerprint> affectedBlocks,
    MonitorOutput output,
    String idempotencyKey,
    String requestHash,
    AuditContext auditContext,
    Instant submittedAt
) {
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public StoredMonitorRun {
    Objects.requireNonNull(runId, "runId");
    Objects.requireNonNull(outputId, "outputId");
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(revisionId, "revisionId");
    if (revisionHash == null || !HASH.matcher(revisionHash).matches()) {
      throw new IllegalArgumentException("Monitor revision hash must be lowercase SHA-256");
    }
    Objects.requireNonNull(targetBlockId, "targetBlockId");
    if (neighborCount < 1 || neighborCount > MonitorModule.MAX_NEIGHBORS) {
      throw new IllegalArgumentException("Monitor neighbor count must be 1 or 2");
    }
    if (monitorVersion == null || monitorVersion.isBlank()) {
      throw new IllegalArgumentException("Monitor version cannot be blank");
    }
    if (ruleVersion == null || ruleVersion.isBlank()) {
      throw new IllegalArgumentException("Monitor rule version cannot be blank");
    }
    affectedBlocks = List.copyOf(affectedBlocks);
    StoredMonitorRunValidation.validate(outputId, novelId, revisionId, revisionHash, targetBlockId, neighborCount, monitorVersion, ruleVersion, affectedBlocks, output, idempotencyKey, requestHash, auditContext, submittedAt);
  }

  public static String requestHash(
      Ids.NovelId novelId,
      Ids.RevisionId revisionId,
      String revisionHash,
      Ids.BlockId targetBlockId,
      int neighborCount,
      String monitorVersion,
      String ruleVersion,
      List<MonitorBlockFingerprint> affectedBlocks,
      MonitorOutput output
  ) {
    return StoredMonitorRunRequestHashFactory.requestHash(novelId, revisionId, revisionHash, targetBlockId, neighborCount, monitorVersion, ruleVersion, affectedBlocks, output);
  }

  public Map<String, Object> canonicalValue() {
    return StoredMonitorRunCanonicalValueAction.canonicalValue(this);
  }
}

package dev.storyblock.monitor;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    if (affectedBlocks.isEmpty() || affectedBlocks.size() > 5) {
      throw new IllegalArgumentException("Monitor run requires 1 to 5 affected blocks");
    }
    if (new HashSet<>(affectedBlocks.stream()
        .map(MonitorBlockFingerprint::blockId).toList()).size()
        != affectedBlocks.size()) {
      throw new IllegalArgumentException("Monitor affected block IDs must be unique");
    }
    if (affectedBlocks.stream().noneMatch(block -> block.blockId().equals(targetBlockId))) {
      throw new IllegalArgumentException("Monitor target must be an affected block");
    }
    Objects.requireNonNull(output, "output");
    if ((output.kind() == MonitorOutputKind.FINDING
        && !(outputId instanceof Ids.MonitorIssueId))
        || (output.kind() == MonitorOutputKind.PROPOSED_OPERATION
        && !(outputId instanceof Ids.MonitorProposalId))) {
      throw new IllegalArgumentException("Monitor output ID does not match output kind");
    }
    Set<Ids.BlockId> affectedIds = affectedBlocks.stream()
        .map(MonitorBlockFingerprint::blockId)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    Set<Ids.BlockId> evidenceIds = output.evidence().stream()
        .map(MonitorEvidence::blockId)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    if (!affectedIds.equals(evidenceIds)) {
      throw new IllegalArgumentException(
          "Every stored affected monitor block must have evidence"
      );
    }
    if (output instanceof MonitorProposedOperation proposal) {
      if (!proposal.operation().context().novelId().equals(novelId)
          || !proposal.operation().context().baseRevisionId().equals(revisionId)
          || !proposal.operation().context().expectedHeadHash().equals(revisionHash)) {
        throw new IllegalArgumentException(
            "Stored monitor proposal context must match its source revision"
        );
      }
      if (proposal.operation()
          instanceof dev.storyblock.domain.EditOperation.RestoreRevisionContent) {
        throw new IllegalArgumentException(
            "Stored monitor proposals cannot restore global revision content"
        );
      }
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()
        || idempotencyKey.length() > 200) {
      throw new IllegalArgumentException("Monitor idempotency key is invalid");
    }
    if (requestHash == null || !HASH.matcher(requestHash).matches()) {
      throw new IllegalArgumentException("Monitor request hash must be lowercase SHA-256");
    }
    Objects.requireNonNull(auditContext, "auditContext");
    Objects.requireNonNull(submittedAt, "submittedAt");
    if (!submittedAt.equals(auditContext.occurredAt())) {
      throw new IllegalArgumentException("Monitor audit time must match submission time");
    }
    String calculated = requestHash(
        novelId,
        revisionId,
        revisionHash,
        targetBlockId,
        neighborCount,
        monitorVersion,
        ruleVersion,
        affectedBlocks,
        output
    );
    if (!calculated.equals(requestHash)) {
      throw new IllegalArgumentException("Monitor request hash does not match payload");
    }
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

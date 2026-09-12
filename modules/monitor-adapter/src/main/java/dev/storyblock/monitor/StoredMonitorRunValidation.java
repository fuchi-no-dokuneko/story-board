package dev.storyblock.monitor;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import static dev.storyblock.monitor.StoredMonitorRun.*;

final class StoredMonitorRunValidation {
  static void validate(Ids.MonitorOutputId outputId, Ids.NovelId novelId, Ids.RevisionId revisionId, String revisionHash, Ids.BlockId targetBlockId, int neighborCount, String monitorVersion, String ruleVersion, List<MonitorBlockFingerprint> affectedBlocks, MonitorOutput output, String idempotencyKey, String requestHash, AuditContext auditContext, Instant submittedAt) {
    MonitorAffectedBlocksValidation.validate(affectedBlocks, targetBlockId);
        Objects.requireNonNull(output, "output");
        if ((output.kind() == MonitorOutputKind.FINDING
            && !(outputId instanceof Ids.MonitorIssueId))
            || (output.kind() == MonitorOutputKind.PROPOSED_OPERATION
            && !(outputId instanceof Ids.MonitorProposalId))) {
          throw new IllegalArgumentException("Monitor output ID does not match output kind");
        }
        MonitorOutputBindingValidation.validate(affectedBlocks, output, novelId, revisionId, revisionHash);
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
}

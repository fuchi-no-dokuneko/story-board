package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.*;
import dev.storyblock.security.AuditContext;
import java.util.List;
import java.util.Objects;

final class MonitorServiceSubmitAction {
  static MonitorSubmissionResult submit(MonitorService self, Ids.NovelId novelId, Ids.RevisionId revisionId, String revisionHash, Ids.BlockId targetBlockId, int neighborCount, String ruleVersion, List<Ids.BlockId> affectedBlockIds, MonitorOutput output, String idempotencyKey, AuditContext auditContext)  {
    Objects.requireNonNull(output, "output");
    Objects.requireNonNull(auditContext, "auditContext");
    if (!self.currentRuleVersion.equals(ruleVersion)) {
      throw new IllegalArgumentException("Monitor submission rule version is not current");
    }
    MonitorPacket packet = self.packet(
        novelId, revisionId, revisionHash, targetBlockId, neighborCount
    );
    List<MonitorBlockFingerprint> affectedFingerprints = MonitorAffectedWindow.validate(packet, affectedBlockIds, targetBlockId, output);
    String requestHash = StoredMonitorRun.requestHash(
        novelId,
        revisionId,
        revisionHash,
        targetBlockId,
        neighborCount,
        MonitorModule.VERSION,
        ruleVersion,
        affectedFingerprints,
        output
    );
    Ids.MonitorOutputId outputId = output.kind() == MonitorOutputKind.FINDING
        ? Ids.MonitorIssueId.create()
        : Ids.MonitorProposalId.create();
    StoredMonitorRun candidate = new StoredMonitorRun(
        Ids.MonitorRunId.create(),
        outputId,
        novelId,
        revisionId,
        revisionHash,
        targetBlockId,
        neighborCount,
        MonitorModule.VERSION,
        ruleVersion,
        affectedFingerprints,
        output,
        idempotencyKey,
        requestHash,
        auditContext,
        auditContext.occurredAt()
    );
    MonitorSaveResult saved = self.monitors.saveMonitorRun(candidate);
    return new MonitorSubmissionResult(
        self.status(saved.run()), saved.idempotentReplay()
    );
  }
}

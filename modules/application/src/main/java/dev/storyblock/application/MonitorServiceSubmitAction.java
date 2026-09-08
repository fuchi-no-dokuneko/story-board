package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.*;
import dev.storyblock.security.AuditContext;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    List<Ids.BlockId> requestedAffected = List.copyOf(affectedBlockIds);
    Set<Ids.BlockId> affected = new LinkedHashSet<>(requestedAffected);
    if (affected.isEmpty() || affected.size() != requestedAffected.size()
        || affected.size() > 5 || !affected.contains(targetBlockId)) {
      throw new IllegalArgumentException(
          "Monitor affected block IDs must be unique, include the target, and number 1 to 5"
      );
    }

    Map<Ids.BlockId, MonitorBlockFingerprint> windowFingerprints = new LinkedHashMap<>();
    packet.localInvariants().windowBlocks().forEach(fingerprint ->
        windowFingerprints.put(fingerprint.blockId(), fingerprint)
    );
    if (!windowFingerprints.keySet().containsAll(affected)) {
      throw new IllegalArgumentException(
          "Monitor affected block IDs must remain inside the supplied packet window"
      );
    }
    MonitorServiceValidateEvidence.validateEvidence(packet, affected, output);
    if (output instanceof MonitorProposedOperation proposal) {
      MonitorServiceValidateProposal.validateProposal(packet, affected, proposal.operation());
    }

    List<MonitorBlockFingerprint> affectedFingerprints = windowFingerprints.values()
        .stream()
        .filter(fingerprint -> affected.contains(fingerprint.blockId()))
        .toList();
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

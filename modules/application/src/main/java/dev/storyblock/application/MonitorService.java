package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.*;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredRevision;
import java.util.List;
import java.util.Objects;

public final class MonitorService {
  final RevisionStore revisions;
  final MonitorStore monitors;
  final MonitorPacketFactory packets;
  final String currentRuleVersion;

  public MonitorService(RevisionStore revisions, MonitorStore monitors) {
    this(revisions, monitors, new MonitorPacketFactory(), MonitorModule.RULE_VERSION);
  }

  MonitorService(
      RevisionStore revisions,
      MonitorStore monitors,
      MonitorPacketFactory packets,
      String currentRuleVersion
  ) {
    this.revisions = Objects.requireNonNull(revisions, "revisions");
    this.monitors = Objects.requireNonNull(monitors, "monitors");
    this.packets = Objects.requireNonNull(packets, "packets");
    if (currentRuleVersion == null || currentRuleVersion.isBlank()) {
      throw new IllegalArgumentException("Current monitor rule version cannot be blank");
    }
    this.currentRuleVersion = currentRuleVersion;
  }

  public MonitorPacket packet(
      Ids.NovelId novelId,
      Ids.RevisionId revisionId,
      String revisionHash,
      Ids.BlockId targetBlockId,
      int neighborCount
  ) {
    StoredRevision revision = requireRevision(novelId, revisionId, revisionHash);
    return packets.create(
        revision.manifest(), revision.contentHash(), targetBlockId, neighborCount
    );
  }

  public MonitorSubmissionResult submit(
      Ids.NovelId novelId,
      Ids.RevisionId revisionId,
      String revisionHash,
      Ids.BlockId targetBlockId,
      int neighborCount,
      String ruleVersion,
      List<Ids.BlockId> affectedBlockIds,
      MonitorOutput output,
      String idempotencyKey,
      AuditContext auditContext
  ) {
    return MonitorServiceSubmitAction.submit(this, novelId, revisionId, revisionHash, targetBlockId, neighborCount, ruleVersion, affectedBlockIds, output, idempotencyKey, auditContext);
  }

  public MonitorRunStatus getStatus(
      Ids.NovelId novelId,
      Ids.MonitorRunId runId
  ) {
    return status(monitors.getMonitorRun(novelId, runId));
  }

  MonitorRunStatus status(StoredMonitorRun run) {
    return MonitorServiceStatusAction.status(this, run);
  }

  StoredRevision requireRevision(
      Ids.NovelId novelId,
      Ids.RevisionId revisionId,
      String revisionHash
  ) {
    return MonitorServiceRequireRevisionAction.requireRevision(this, novelId, revisionId, revisionHash);
  }

}

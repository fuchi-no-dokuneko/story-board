package dev.storyblock.monitor;

import dev.storyblock.detector.DetectorFinding;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record MonitorPacket(
    Ids.NovelId novelId,
    Ids.RevisionId revisionId,
    String revisionHash,
    String monitorVersion,
    String ruleVersion,
    String detectorRuleVersion,
    Ids.BlockId targetBlockId,
    int neighborCount,
    RenderPacket renderPacket,
    List<DetectorFinding> detectorFindings,
    MonitorLocalInvariants localInvariants,
    List<MonitorTool> allowedTools
) {
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public MonitorPacket {
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(revisionId, "revisionId");
    if (revisionHash == null || !HASH.matcher(revisionHash).matches()) {
      throw new IllegalArgumentException("Monitor packet hash must be lowercase SHA-256");
    }
    if (monitorVersion == null || monitorVersion.isBlank()
        || ruleVersion == null || ruleVersion.isBlank()
        || detectorRuleVersion == null || detectorRuleVersion.isBlank()) {
      throw new IllegalArgumentException("Monitor packet versions cannot be blank");
    }
    Objects.requireNonNull(targetBlockId, "targetBlockId");
    if (neighborCount < 1 || neighborCount > MonitorModule.MAX_NEIGHBORS) {
      throw new IllegalArgumentException("Monitor neighbor count must be 1 or 2");
    }
    Objects.requireNonNull(renderPacket, "renderPacket");
    detectorFindings = List.copyOf(detectorFindings);
    Objects.requireNonNull(localInvariants, "localInvariants");
    allowedTools = List.copyOf(allowedTools);
    MonitorPacketValidation.validate(novelId, revisionId, revisionHash, targetBlockId, renderPacket, detectorFindings, localInvariants, allowedTools);
  }

  public Map<String, Object> canonicalValue() {
    return MonitorPacketCanonicalValueAction.canonicalValue(this);
  }
}

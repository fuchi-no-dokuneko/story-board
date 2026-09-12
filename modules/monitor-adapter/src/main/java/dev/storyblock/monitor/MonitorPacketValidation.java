package dev.storyblock.monitor;

import dev.storyblock.detector.DetectorFinding;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Set;
import static dev.storyblock.monitor.MonitorPacket.*;

final class MonitorPacketValidation {
  static void validate(Ids.NovelId novelId, Ids.RevisionId revisionId, String revisionHash, Ids.BlockId targetBlockId, RenderPacket renderPacket, List<DetectorFinding> detectorFindings, MonitorLocalInvariants localInvariants, List<MonitorTool> allowedTools) {
    if (!allowedTools.equals(List.of(
            MonitorTool.SUBMIT_FINDING,
            MonitorTool.SUBMIT_PROPOSED_OPERATION
        ))) {
          throw new IllegalArgumentException("Monitor packet has unsupported tools");
        }
        if (!novelId.equals(renderPacket.novelId())
            || !revisionId.equals(renderPacket.revisionId())
            || !revisionHash.equals(renderPacket.revisionHash())
            || renderPacket.blocks().stream()
                .noneMatch(block -> block.blockId().equals(targetBlockId))) {
          throw new IllegalArgumentException("Monitor packet render identity is inconsistent");
        }
        List<Ids.BlockId> renderedIds = renderPacket.blocks().stream()
            .map(block -> block.blockId()).toList();
        List<Ids.BlockId> fingerprintIds = localInvariants.windowBlocks().stream()
            .map(MonitorBlockFingerprint::blockId).toList();
        if (!renderedIds.equals(fingerprintIds)) {
          throw new IllegalArgumentException(
              "Monitor render blocks and fingerprints must align"
          );
        }
        MonitorBlockFingerprint targetFingerprint = localInvariants.windowBlocks().stream()
            .filter(block -> block.blockId().equals(targetBlockId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Monitor invariants do not contain the target block"
            ));
        if (!targetFingerprint.blockVersionId().equals(
            localInvariants.targetBlockVersionId()
        )) {
          throw new IllegalArgumentException(
              "Monitor target block version does not match its fingerprint"
          );
        }
        Set<Ids.BlockId> windowIds = Set.copyOf(renderedIds);
        for (DetectorFinding finding : detectorFindings) {
          if (!windowIds.containsAll(finding.affectedBlockIds())
              || !windowIds.containsAll(finding.contextBlockIds())) {
            throw new IllegalArgumentException(
                "Monitor detector findings must remain inside the packet window"
            );
          }
        }
  }
}

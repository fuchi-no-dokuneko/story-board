package dev.storyblock.monitor;

import dev.storyblock.detector.DetectorFinding;
import dev.storyblock.domain.CanonicalValues;
import java.util.Map;

final class MonitorPacketCanonicalValueAction {
    static Map<String, Object> canonicalValue(MonitorPacket self)  {
        return CanonicalValues.freezeMap(Map.ofEntries(
                Map.entry("allowed_tools", self.allowedTools().stream()
                        .map(MonitorTool::canonicalName).toList()),
                Map.entry("detector_findings", self.detectorFindings().stream()
                        .map(DetectorFinding::canonicalValue).toList()),
                Map.entry("detector_rule_version", self.detectorRuleVersion()),
                Map.entry("local_invariants", self.localInvariants().canonicalValue()),
                Map.entry("monitor_version", self.monitorVersion()),
                Map.entry("neighbor_count", self.neighborCount()),
                Map.entry("novel_id", self.novelId().value()),
                Map.entry("render_packet", self.renderPacket().canonicalValue()),
                Map.entry("revision_hash", self.revisionHash()),
                Map.entry("revision_id", self.revisionId().value()),
                Map.entry("rule_version", self.ruleVersion()),
                Map.entry("target_block_id", self.targetBlockId().value())
        ), "monitor_packet");
    }
}

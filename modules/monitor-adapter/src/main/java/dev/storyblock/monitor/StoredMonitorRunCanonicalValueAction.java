package dev.storyblock.monitor;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StoredMonitorRunCanonicalValueAction {
    static Map<String, Object> canonicalValue(StoredMonitorRun self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("monitor_run_id", self.runId().value());
        value.put("output_id", self.outputId().value());
        value.put("output_kind", self.output().kind().canonicalName());
        value.put("novel_id", self.novelId().value());
        value.put("revision_id", self.revisionId().value());
        value.put("revision_hash", self.revisionHash());
        value.put("target_block_id", self.targetBlockId().value());
        value.put("neighbor_count", self.neighborCount());
        value.put("monitor_version", self.monitorVersion());
        value.put("rule_version", self.ruleVersion());
        value.put("affected_blocks", self.affectedBlocks().stream()
                .map(MonitorBlockFingerprint::canonicalValue).toList());
        value.put("output", self.output().canonicalValue());
        value.put("request_hash", self.requestHash());
        value.put("submitted_at", self.submittedAt().toString());
        return CanonicalValues.freezeMap(value, "stored_monitor_run");
    }
}

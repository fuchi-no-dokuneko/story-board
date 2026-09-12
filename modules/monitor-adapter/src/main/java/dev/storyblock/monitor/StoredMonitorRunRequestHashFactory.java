package dev.storyblock.monitor;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StoredMonitorRunRequestHashFactory {
    static String requestHash(Ids.NovelId novelId, Ids.RevisionId revisionId, String revisionHash, Ids.BlockId targetBlockId, int neighborCount, String monitorVersion, String ruleVersion, List<MonitorBlockFingerprint> affectedBlocks, MonitorOutput output)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("affected_blocks", affectedBlocks.stream()
                .map(MonitorBlockFingerprint::canonicalValue).toList());
        value.put("monitor_version", monitorVersion);
        value.put("neighbor_count", neighborCount);
        value.put("novel_id", novelId.value());
        value.put("output", output.canonicalValue());
        value.put("revision_hash", revisionHash);
        value.put("revision_id", revisionId.value());
        value.put("rule_version", ruleVersion);
        value.put("target_block_id", targetBlockId.value());
        return CanonicalJson.hash(value);
    }
}

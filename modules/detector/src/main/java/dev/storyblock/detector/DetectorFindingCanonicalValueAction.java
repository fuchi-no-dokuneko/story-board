package dev.storyblock.detector;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.Map;

final class DetectorFindingCanonicalValueAction {
    static Map<String, Object> canonicalValue(DetectorFinding self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("finding_id", self.findingId().value());
        value.put("code", self.code().name());
        value.put("severity", self.severity().canonicalName());
        value.put("revision_id", self.revisionId().value());
        value.put("revision_hash", self.revisionHash());
        value.put("rule_version", self.ruleVersion());
        value.put(
                "affected_block_ids",
                self.affectedBlockIds().stream().map(Ids.BlockId::value).toList()
        );
        value.put(
                "affected_scene_ids",
                self.affectedSceneIds().stream().map(Ids.SceneId::value).toList()
        );
        value.put(
                "context_block_ids",
                self.contextBlockIds().stream().map(Ids.BlockId::value).toList()
        );
        value.put("evidence", self.evidence());
        return CanonicalValues.freezeMap(value, "detector_finding");
    }
}

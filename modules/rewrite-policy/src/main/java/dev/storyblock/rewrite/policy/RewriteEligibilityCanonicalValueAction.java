package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnomalyDecision;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteEligibilityCanonicalValueAction {
    static Map<String, Object> canonicalValue(RewriteEligibility self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("affected_block_ids", self.affectedBlockIds().stream()
                .map(Ids.BlockId::value).toList());
        value.put("analysis_id", self.analysisId().value());
        value.put("analysis_result_hash", self.analysisResultHash());
        value.put("analyzer_contract_hash", self.analyzerContractHash());
        value.put("decisions", self.decisions().stream()
                .map(StyleAnomalyDecision::canonicalValue).toList());
        value.put("finding_ids", self.findingIds());
        value.put("novel_id", self.novelId().value());
        value.put("profile_id", self.profileId().value());
        value.put("profile_version_hash", self.profileVersionHash());
        value.put("profile_version_id", self.profileVersionId().value());
        value.put("revision_hash", self.revisionHash());
        value.put("revision_id", self.revisionId().value());
        value.put("window_configuration_hash", self.windowConfigurationHash());
        return CanonicalValues.freezeMap(value, "rewrite_eligibility");
    }
}

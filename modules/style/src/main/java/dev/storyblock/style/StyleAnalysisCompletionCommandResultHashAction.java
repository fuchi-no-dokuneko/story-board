package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisCompletionCommandResultHashAction {
    static String resultHash(StyleAnalysisCompletionCommand self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", self.trace().analysisId().value());
        value.put("analyzer_contract_hash", self.analyzerContractHash());
        value.put("profile_version_hash", self.profileVersionHash());
        value.put("snapshot_hash", self.snapshotHash());
        value.put("summary", self.summary().canonicalValue());
        value.put("trace", self.trace().metadataValue());
        value.put("window_configuration_hash", self.windowConfigurationHash());
        value.put("windows", self.windows().stream()
                .map(StyleAnalysisWindowFinding::canonicalValue).toList());
        return CanonicalJson.hash(value);
    }
}

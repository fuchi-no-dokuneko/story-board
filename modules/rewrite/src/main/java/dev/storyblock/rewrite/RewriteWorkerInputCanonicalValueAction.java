package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteWorkerInputCanonicalValueAction {
    static Map<String, Object> canonicalValue(RewriteWorkerInput self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", self.analysisId().value());
        value.put("analyzer_contract_hash", self.analyzerContractHash());
        value.put("blocks", self.blocks().stream()
                .map(RewriteSourceBlock::canonicalValue).toList());
        value.put("constraints", self.constraints().canonicalValue());
        value.put("finding_ids", self.findingIds());
        value.put("novel_id", self.novelId().value());
        value.put("profile_version_hash", self.profileVersionHash());
        value.put("profile_version_id", self.profileVersionId().value());
        value.put("proposal_id", self.proposalId().value());
        value.put("revision_hash", self.revisionHash());
        value.put("revision_id", self.revisionId().value());
        value.put("schema_version", RewriteModule.INPUT_SCHEMA_VERSION);
        value.put("window_configuration_hash", self.windowConfigurationHash());
        return CanonicalValues.freezeMap(value, "rewrite_worker_input");
    }
}

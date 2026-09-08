package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteTextProposalValueWithoutProposalHashAction {
    static Map<String, Object> valueWithoutProposalHash(RewriteTextProposal self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("candidates", self.candidates().stream()
                .map(RewriteCandidateBlock::canonicalValue).toList());
        value.put("created_at", self.createdAt().toString());
        value.put("input", self.input().canonicalValue());
        value.put("input_hash", self.input().inputHash());
        value.put("model_id", self.modelId());
        value.put("model_response_hash", self.modelResponseHash());
        value.put("proposal_id", self.input().proposalId().value());
        value.put("schema_version", RewriteModule.PROPOSAL_SCHEMA_VERSION);
        return CanonicalValues.freezeMap(value, "rewrite_text_proposal_content");
    }
}

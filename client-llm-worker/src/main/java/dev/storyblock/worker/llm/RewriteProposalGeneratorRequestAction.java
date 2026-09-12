package dev.storyblock.worker.llm;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.rewrite.RewriteWorkerInput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RewriteProposalGeneratorRequestAction {
    static Map<String, Object> request(RewriteProposalGenerator self, RewriteWorkerInput input)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("input", input.modelValue());
        value.put("instructions", RewriteProposalGenerator.INSTRUCTIONS);
        value.put("model", self.modelId);
        value.put("protocol_version", RewriteModule.MODEL_PROTOCOL_VERSION);
        value.put("response_schema", RewriteProposalGeneratorResponseSchema.responseSchema(
                self.modelId, input.constraints().maxChangedBlocks()
        ));
        value.put("tools", List.of());
        return CanonicalValues.freezeMap(value, "rewrite_model_request");
    }
}

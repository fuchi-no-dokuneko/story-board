package dev.storyblock.worker.llm;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.rewrite.RewriteModelResponse;
import java.util.Map;

final class RewriteProposalGeneratorParseResponse {
    static RewriteModelResponse parseResponse(byte[] responseBytes) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = CanonicalJson.mapper().readValue(
                    responseBytes, Map.class
            );
            return RewriteModelResponse.fromCanonical(value);
        } catch (RuntimeException invalid) {
            throw new LlmWorkerProtocolException(
                    "Model response does not match the rewrite protocol"
            );
        }
    }
}

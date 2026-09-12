package dev.storyblock.worker.llm;

import dev.storyblock.rewrite.RewriteModule;
import java.util.List;
import java.util.Map;

final class RewriteProposalGeneratorResponseSchema {
    static Map<String, Object> responseSchema(
            String modelId,
            int maxChangedBlocks
    ) {
        Map<String, Object> replacement = Map.of(
                "additionalProperties", false,
                "properties", Map.of(
                        "block_id", Map.of("type", "string"),
                        "text", Map.of("type", "string")
                ),
                "required", List.of("block_id", "text"),
                "type", "object"
        );
        Map<String, Object> output = Map.of(
                "additionalProperties", false,
                "properties", Map.of(
                        "input_hash", Map.of("type", "string"),
                        "replacements", Map.of(
                                "items", replacement,
                                "maxItems", maxChangedBlocks,
                                "minItems", 1,
                                "type", "array"
                        )
                ),
                "required", List.of("input_hash", "replacements"),
                "type", "object"
        );
        return Map.of(
                "additionalProperties", false,
                "properties", Map.of(
                        "model", Map.of("const", modelId),
                        "output", output,
                        "protocol_version", Map.of(
                                "const", RewriteModule.MODEL_PROTOCOL_VERSION
                        )
                ),
                "required", List.of("model", "output", "protocol_version"),
                "type", "object"
        );
    }
}

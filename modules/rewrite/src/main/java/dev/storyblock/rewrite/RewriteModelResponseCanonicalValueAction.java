package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteModelResponseCanonicalValueAction {
    static Map<String, Object> canonicalValue(RewriteModelResponse self)  {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("input_hash", self.inputHash());
        output.put("replacements", self.replacements().stream()
                .map(RewriteModelReplacement::canonicalValue).toList());
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("model", self.modelId());
        value.put("output", output);
        value.put("protocol_version", RewriteModule.MODEL_PROTOCOL_VERSION);
        return CanonicalValues.freezeMap(value, "rewrite_model_response");
    }
}

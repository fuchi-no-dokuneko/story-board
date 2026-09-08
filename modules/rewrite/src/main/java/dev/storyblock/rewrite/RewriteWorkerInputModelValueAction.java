package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteWorkerInputModelValueAction {
    static Map<String, Object> modelValue(RewriteWorkerInput self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("blocks", self.blocks().stream()
                .map(RewriteSourceBlock::canonicalValue).toList());
        value.put("constraints", self.constraints().canonicalValue());
        value.put("input_hash", self.inputHash());
        value.put("schema_version", RewriteModule.INPUT_SCHEMA_VERSION);
        return CanonicalValues.freezeMap(value, "rewrite_model_input");
    }
}

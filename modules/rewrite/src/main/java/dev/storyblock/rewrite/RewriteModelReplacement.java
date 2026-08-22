package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RewriteModelReplacement(Ids.BlockId blockId, String text) {
    private static final Set<String> FIELDS = Set.of("block_id", "text");

    public RewriteModelReplacement {
        Objects.requireNonNull(blockId, "blockId");
        UnicodeText.validateBlock(text);
    }

    public static RewriteModelReplacement fromCanonical(Map<String, Object> value) {
        RewriteCanonical.requireKeys(value, FIELDS, "rewrite_model_replacement");
        return new RewriteModelReplacement(
                new Ids.BlockId(RewriteCanonical.string(
                        value, "block_id", "rewrite_model_replacement"
                )),
                RewriteCanonical.string(value, "text", "rewrite_model_replacement")
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_id", blockId.value());
        value.put("text", text);
        return CanonicalValues.freezeMap(value, "rewrite_model_replacement");
    }
}

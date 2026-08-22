package dev.storyblock.rewrite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record RewriteSourceBlock(
        Ids.BlockId blockId,
        Ids.BlockVersionId blockVersionId,
        String textHash,
        String text,
        boolean editable
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "block_id", "block_version_id", "editable", "text", "text_hash"
    );

    public RewriteSourceBlock {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(blockVersionId, "blockVersionId");
        if (textHash == null || !HASH.matcher(textHash).matches()) {
            throw new IllegalArgumentException("Rewrite source text hash is invalid");
        }
        UnicodeText.validateBlock(text);
        if (!CanonicalJson.hash(text).equals(textHash)) {
            throw new IllegalArgumentException("Rewrite source text hash does not match text");
        }
    }

    public static RewriteSourceBlock create(
            Ids.BlockId blockId,
            Ids.BlockVersionId blockVersionId,
            String text,
            boolean editable
    ) {
        return new RewriteSourceBlock(
                blockId, blockVersionId, CanonicalJson.hash(text), text, editable
        );
    }

    public static RewriteSourceBlock fromCanonical(Map<String, Object> value) {
        RewriteCanonical.requireKeys(value, FIELDS, "rewrite_source_block");
        return new RewriteSourceBlock(
                new Ids.BlockId(RewriteCanonical.string(
                        value, "block_id", "rewrite_source_block"
                )),
                new Ids.BlockVersionId(RewriteCanonical.string(
                        value, "block_version_id", "rewrite_source_block"
                )),
                RewriteCanonical.string(value, "text_hash", "rewrite_source_block"),
                RewriteCanonical.string(value, "text", "rewrite_source_block"),
                RewriteCanonical.bool(value, "editable", "rewrite_source_block")
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_id", blockId.value());
        value.put("block_version_id", blockVersionId.value());
        value.put("editable", editable);
        value.put("text", text);
        value.put("text_hash", textHash);
        return CanonicalValues.freezeMap(value, "rewrite_source_block");
    }
}

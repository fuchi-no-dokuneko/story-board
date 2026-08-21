package dev.storyblock.monitor;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record MonitorBlockFingerprint(
        Ids.BlockId blockId,
        Ids.BlockVersionId blockVersionId,
        String contentHash
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "block_id", "block_version_id", "content_hash"
    );

    public MonitorBlockFingerprint {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(blockVersionId, "blockVersionId");
        if (contentHash == null || !HASH.matcher(contentHash).matches()) {
            throw new IllegalArgumentException("Monitor block hash must be lowercase SHA-256");
        }
    }

    public static MonitorBlockFingerprint from(NarrativeBlock block) {
        Objects.requireNonNull(block, "block");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("block_version_id", block.versionId().value());
        content.put("extensions", block.extensions());
        content.put("meta", block.metadata().fields());
        content.put("text", block.text());
        return new MonitorBlockFingerprint(
                block.id(), block.versionId(), CanonicalJson.hash(content)
        );
    }

    public static MonitorBlockFingerprint fromCanonical(Map<String, Object> value) {
        Objects.requireNonNull(value, "value");
        if (!value.keySet().equals(FIELDS)) {
            throw new IllegalArgumentException("Monitor block fingerprint fields are invalid");
        }
        return new MonitorBlockFingerprint(
                new Ids.BlockId(string(value, "block_id")),
                new Ids.BlockVersionId(string(value, "block_version_id")),
                string(value, "content_hash")
        );
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "block_id", blockId.value(),
                "block_version_id", blockVersionId.value(),
                "content_hash", contentHash
        ), "monitor_block_fingerprint");
    }

    private static String string(Map<String, Object> value, String field) {
        Object raw = value.get(field);
        if (!(raw instanceof String text)) {
            throw new IllegalArgumentException(
                    "Monitor block fingerprint " + field + " must be a string"
            );
        }
        return text;
    }
}

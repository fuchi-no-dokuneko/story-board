package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record RewriteFactDifference(
        Ids.BlockId blockId,
        ProtectedFactKind kind,
        String valueHash,
        int sourceCount,
        int candidateCount,
        RewriteFactDisposition disposition
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public RewriteFactDifference {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(kind, "kind");
        if (valueHash == null || !HASH.matcher(valueHash).matches()
                || sourceCount < 0 || candidateCount < 0
                || sourceCount == candidateCount) {
            throw new IllegalArgumentException("Rewrite fact difference is invalid");
        }
        Objects.requireNonNull(disposition, "disposition");
        if (disposition != kind.changedDisposition()) {
            throw new IllegalArgumentException(
                    "Rewrite fact disposition does not match its policy"
            );
        }
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "block_id", blockId.value(),
                "candidate_count", candidateCount,
                "disposition", disposition.canonicalName(),
                "kind", kind.canonicalName(),
                "source_count", sourceCount,
                "value_hash", valueHash
        ), "rewrite_fact_difference");
    }
}

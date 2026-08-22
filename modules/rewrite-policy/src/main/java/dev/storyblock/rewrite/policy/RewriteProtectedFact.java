package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.CanonicalValues;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record RewriteProtectedFact(
        ProtectedFactKind kind,
        String valueHash,
        int count
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public RewriteProtectedFact {
        Objects.requireNonNull(kind, "kind");
        if (valueHash == null || !HASH.matcher(valueHash).matches() || count < 1) {
            throw new IllegalArgumentException("Protected rewrite fact is invalid");
        }
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "count", count,
                "kind", kind.canonicalName(),
                "value_hash", valueHash
        ), "rewrite_protected_fact");
    }
}

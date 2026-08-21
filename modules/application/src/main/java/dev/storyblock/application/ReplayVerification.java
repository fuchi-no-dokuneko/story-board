package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ReplayVerification(
        Ids.NovelId novelId,
        Ids.RevisionId headRevisionId,
        String expectedHash,
        String actualHash,
        long replayedOperations,
        boolean valid,
        String detail
) {
    public ReplayVerification {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(detail, "detail");
        if (replayedOperations < 0) {
            throw new IllegalArgumentException("Replayed operation count cannot be negative");
        }
        if (valid) {
            Objects.requireNonNull(headRevisionId, "headRevisionId");
            Objects.requireNonNull(expectedHash, "expectedHash");
            if (!expectedHash.equals(actualHash)) {
                throw new IllegalArgumentException(
                        "A valid replay must reproduce the expected hash"
                );
            }
        }
    }

    public Map<String, Object> contractFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("novel_id", novelId.value());
        fields.put("head_revision_id", headRevisionId == null ? null : headRevisionId.value());
        fields.put("expected_hash", expectedHash);
        fields.put("actual_hash", actualHash);
        fields.put("replayed_operations", replayedOperations);
        fields.put("valid", valid);
        fields.put("detail", detail);
        return java.util.Collections.unmodifiableMap(fields);
    }
}

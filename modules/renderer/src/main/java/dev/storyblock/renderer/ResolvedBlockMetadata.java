package dev.storyblock.renderer;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ResolvedBlockMetadata(
        Ids.BlockId blockId,
        Map<String, Object> before,
        List<Map<String, Object>> events,
        Map<String, Object> after
) {
    public ResolvedBlockMetadata {
        Objects.requireNonNull(blockId, "blockId");
        before = CanonicalValues.freezeMap(before, "resolved_meta.before");
        events = events.stream()
                .map(event -> CanonicalValues.freezeMap(event, "resolved_meta.event"))
                .toList();
        after = CanonicalValues.freezeMap(after, "resolved_meta.after");
    }
}

package dev.storyblock.monitor;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MonitorLocalInvariants(
        Ids.SceneId targetSceneId,
        Ids.BlockVersionId targetBlockVersionId,
        List<MonitorBlockFingerprint> windowBlocks
) {
    public static final int MAX_BLOCK_GRAPHEMES = 100;
    public static final int MIN_SENTENCES = 1;
    public static final int MAX_SENTENCES = 2;
    public static final int MAX_DIRECT_SPEAKERS = 1;

    public MonitorLocalInvariants {
        Objects.requireNonNull(targetSceneId, "targetSceneId");
        Objects.requireNonNull(targetBlockVersionId, "targetBlockVersionId");
        windowBlocks = List.copyOf(windowBlocks);
        if (windowBlocks.isEmpty() || windowBlocks.size() > 5) {
            throw new IllegalArgumentException("Monitor window must contain 1 to 5 blocks");
        }
        if (new HashSet<>(windowBlocks.stream()
                .map(MonitorBlockFingerprint::blockId).toList()).size() != windowBlocks.size()) {
            throw new IllegalArgumentException("Monitor window block IDs must be unique");
        }
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.ofEntries(
                Map.entry("affected_ids_must_be_within_window", true),
                Map.entry("evidence_required", true),
                Map.entry("max_block_graphemes", MAX_BLOCK_GRAPHEMES),
                Map.entry("max_direct_speakers", MAX_DIRECT_SPEAKERS),
                Map.entry("max_sentences", MAX_SENTENCES),
                Map.entry("min_sentences", MIN_SENTENCES),
                Map.entry("target_block_version_id", targetBlockVersionId.value()),
                Map.entry("target_scene_id", targetSceneId.value()),
                Map.entry("window_blocks", windowBlocks.stream()
                        .map(MonitorBlockFingerprint::canonicalValue).toList())
        ), "monitor_local_invariants");
    }
}

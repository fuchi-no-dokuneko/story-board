package dev.storyblock.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BlockProvenanceMapping(Map<BlockVersionRef, List<Ids.BlockId>> sourceToResults) {
    public BlockProvenanceMapping {
        Map<BlockVersionRef, List<Ids.BlockId>> frozen = new LinkedHashMap<>();
        for (Map.Entry<BlockVersionRef, List<Ids.BlockId>> entry : sourceToResults.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                throw new IllegalArgumentException("Every provenance source requires result block IDs");
            }
            List<Ids.BlockId> results = List.copyOf(entry.getValue());
            if (results.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Provenance result block IDs cannot be null");
            }
            frozen.put(entry.getKey(), results);
        }
        if (frozen.isEmpty()) {
            throw new IllegalArgumentException("Block provenance mapping cannot be empty");
        }
        sourceToResults = java.util.Collections.unmodifiableMap(frozen);
    }

    public static BlockProvenanceMapping split(
            BlockVersionRef source,
            List<BlockDraft> results
    ) {
        return new BlockProvenanceMapping(Map.of(
                source,
                results.stream().map(BlockDraft::id).toList()
        ));
    }

    public static BlockProvenanceMapping merge(
            List<BlockVersionRef> sources,
            BlockDraft result
    ) {
        Map<BlockVersionRef, List<Ids.BlockId>> mapping = new LinkedHashMap<>();
        for (BlockVersionRef source : sources) {
            mapping.put(source, List.of(result.id()));
        }
        return new BlockProvenanceMapping(mapping);
    }
}

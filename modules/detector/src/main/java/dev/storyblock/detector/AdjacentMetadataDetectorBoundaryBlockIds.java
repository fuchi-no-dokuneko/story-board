package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeScene;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AdjacentMetadataDetectorBoundaryBlockIds {
    static List<Ids.BlockId> boundaryBlockIds(
            NarrativeScene previous,
            NarrativeScene current
    ) {
        Set<Ids.BlockId> result = new LinkedHashSet<>();
        if (!previous.blocks().isEmpty()) {
            result.add(previous.blocks().getLast().id());
        }
        if (!current.blocks().isEmpty()) {
            result.add(current.blocks().getFirst().id());
        }
        return List.copyOf(result);
    }
}

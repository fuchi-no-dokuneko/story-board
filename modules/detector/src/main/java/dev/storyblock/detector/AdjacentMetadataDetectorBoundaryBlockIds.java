package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.NarrativeView;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AdjacentMetadataDetectorBoundaryBlockIds {
    static List<Ids.BlockId> boundaryBlockIds(
            NarrativeScene previous,
            NarrativeScene current
    ) {
        Set<Ids.BlockId> result = new LinkedHashSet<>();
        var previousVisible = NarrativeView.visibleBlocks(previous);
        var currentVisible = NarrativeView.visibleBlocks(current);
        if (!previousVisible.isEmpty()) {
            result.add(previousVisible.getLast().id());
        }
        if (!currentVisible.isEmpty()) {
            result.add(currentVisible.getFirst().id());
        }
        return List.copyOf(result);
    }
}

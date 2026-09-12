package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import java.util.List;

public record RevisionDiff(
        List<BlockChange> blockChanges,
        List<SceneSeedChange> sceneSeedChanges
) {
    public RevisionDiff {
        blockChanges = List.copyOf(blockChanges);
        sceneSeedChanges = List.copyOf(sceneSeedChanges);
    }

    public static RevisionDiff empty() {
        return new RevisionDiff(List.of(), List.of());
    }

    public static RevisionDiff between(RevisionManifest base, RevisionManifest candidate) {
        return RevisionDiffBetweenFactory.between(base, candidate);
    }

    record LocatedBlock(Ids.SceneId sceneId, int index, NarrativeBlock block) {
    }
}

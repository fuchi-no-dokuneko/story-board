package dev.storyblock.application;

import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static dev.storyblock.application.RevisionDiff.LocatedBlock;

final class RevisionDiffBetweenFactory {
    static RevisionDiff between(RevisionManifest base, RevisionManifest candidate)  {
        Map<String, LocatedBlock> before = RevisionDiffLocateBlocks.locateBlocks(base);
        Map<String, LocatedBlock> after = RevisionDiffLocateBlocks.locateBlocks(candidate);
        Map<String, Boolean> identities = new TreeMap<>();
        before.keySet().forEach(id -> identities.put(id, Boolean.TRUE));
        after.keySet().forEach(id -> identities.put(id, Boolean.TRUE));

        List<BlockChange> changes = new ArrayList<>();
        for (String identity : identities.keySet()) {
            LocatedBlock oldBlock = before.get(identity);
            LocatedBlock newBlock = after.get(identity);
            if (oldBlock == null) {
                changes.add(RevisionDiffChange.change(BlockChange.Type.ADDED, null, newBlock));
                continue;
            }
            if (newBlock == null) {
                changes.add(RevisionDiffChange.change(BlockChange.Type.DELETED, oldBlock, null));
                continue;
            }
            if (!oldBlock.block().versionId().equals(newBlock.block().versionId())) {
                changes.add(RevisionDiffChange.change(BlockChange.Type.MODIFIED, oldBlock, newBlock));
            }
            if (!oldBlock.sceneId().equals(newBlock.sceneId())
                    || !oldBlock.block().orderKey().equals(newBlock.block().orderKey())) {
                changes.add(RevisionDiffChange.change(BlockChange.Type.MOVED, oldBlock, newBlock));
            }
        }

        Map<String, NarrativeScene> oldScenes = RevisionDiffLocateScenes.locateScenes(base);
        Map<String, NarrativeScene> newScenes = RevisionDiffLocateScenes.locateScenes(candidate);
        List<SceneSeedChange> seedChanges = new ArrayList<>();
        for (String sceneId : oldScenes.keySet()) {
            NarrativeScene oldScene = oldScenes.get(sceneId);
            NarrativeScene newScene = newScenes.get(sceneId);
            if (newScene != null && !java.util.Objects.equals(
                    oldScene.initialMeta(), newScene.initialMeta()
            )) {
                seedChanges.add(new SceneSeedChange(
                        oldScene.id(), oldScene.initialMeta(), newScene.initialMeta()
                ));
            }
        }
        return new RevisionDiff(changes, seedChanges);
    }
}

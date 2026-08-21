package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        Map<String, LocatedBlock> before = locateBlocks(base);
        Map<String, LocatedBlock> after = locateBlocks(candidate);
        Map<String, Boolean> identities = new TreeMap<>();
        before.keySet().forEach(id -> identities.put(id, Boolean.TRUE));
        after.keySet().forEach(id -> identities.put(id, Boolean.TRUE));

        List<BlockChange> changes = new ArrayList<>();
        for (String identity : identities.keySet()) {
            LocatedBlock oldBlock = before.get(identity);
            LocatedBlock newBlock = after.get(identity);
            if (oldBlock == null) {
                changes.add(change(BlockChange.Type.ADDED, null, newBlock));
                continue;
            }
            if (newBlock == null) {
                changes.add(change(BlockChange.Type.DELETED, oldBlock, null));
                continue;
            }
            if (!oldBlock.block().versionId().equals(newBlock.block().versionId())) {
                changes.add(change(BlockChange.Type.MODIFIED, oldBlock, newBlock));
            }
            if (!oldBlock.sceneId().equals(newBlock.sceneId())
                    || !oldBlock.block().orderKey().equals(newBlock.block().orderKey())) {
                changes.add(change(BlockChange.Type.MOVED, oldBlock, newBlock));
            }
        }

        Map<String, NarrativeScene> oldScenes = locateScenes(base);
        Map<String, NarrativeScene> newScenes = locateScenes(candidate);
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

    private static BlockChange change(
            BlockChange.Type type,
            LocatedBlock oldBlock,
            LocatedBlock newBlock
    ) {
        LocatedBlock identity = oldBlock == null ? newBlock : oldBlock;
        return new BlockChange(
                type,
                identity.block().id(),
                oldBlock == null ? null : oldBlock.block().versionId(),
                newBlock == null ? null : newBlock.block().versionId(),
                oldBlock == null ? null : oldBlock.sceneId(),
                newBlock == null ? null : newBlock.sceneId(),
                oldBlock == null ? null : oldBlock.index(),
                newBlock == null ? null : newBlock.index(),
                oldBlock == null ? null : oldBlock.block().text(),
                newBlock == null ? null : newBlock.block().text()
        );
    }

    private static Map<String, LocatedBlock> locateBlocks(RevisionManifest revision) {
        Map<String, LocatedBlock> located = new TreeMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (int index = 0; index < scene.blocks().size(); index++) {
                    NarrativeBlock block = scene.blocks().get(index);
                    located.put(block.id().value(), new LocatedBlock(scene.id(), index, block));
                }
            }
        }
        return located;
    }

    private static Map<String, NarrativeScene> locateScenes(RevisionManifest revision) {
        Map<String, NarrativeScene> located = new TreeMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                located.put(scene.id().value(), scene);
            }
        }
        return located;
    }

    private record LocatedBlock(Ids.SceneId sceneId, int index, NarrativeBlock block) {
    }
}

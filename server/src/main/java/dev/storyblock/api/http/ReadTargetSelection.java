package dev.storyblock.api.http;

import dev.storyblock.domain.*;

final class ReadTargetSelection {
    static ReadTargets select(RevisionManifest revision, EditOperation operation) {
        var targets = new ReadTargets(revision);
        switch (operation) {
            case EditOperation.InsertBlocks op -> targets.point(op.insertionPoint());
            case EditOperation.ReplaceBlockRange op -> targets.range(op.range());
            case EditOperation.DeleteBlockRange op -> targets.range(op.range());
            case EditOperation.SplitBlock op -> targets.range(op.block());
            case EditOperation.MergeBlocks op -> targets.range(op.range());
            case EditOperation.ExtendBlock op -> targets.range(op.block());
            case EditOperation.MoveBlockRange op -> { targets.range(op.range()); targets.point(op.destination()); }
            case EditOperation.CorrectBlockMeta op -> {
                var scene = revision.requireScene(op.sceneId());
                int index = ReadTargets.find(scene, op.block().blockId());
                targets.add(scene, index - 1, index + 2);
            }
            case EditOperation.SetSceneInitialMeta op -> {
                var scene = revision.requireScene(op.sceneId());
                targets.add(scene, 0, scene.blocks().size());
            }
            case EditOperation.RestoreRevisionContent ignored -> {
                for (var chapter : revision.novel().chapters())
                    for (var scene : chapter.scenes()) targets.add(scene, 0, scene.blocks().size());
            }
        }
        return targets;
    }
}

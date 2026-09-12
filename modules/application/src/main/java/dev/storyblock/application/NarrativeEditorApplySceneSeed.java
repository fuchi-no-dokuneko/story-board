package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;

final class NarrativeEditorApplySceneSeed {
    static NarrativeNovel applySceneSeed(
            RevisionManifest base,
            EditOperation.SetSceneInitialMeta operation
    ) {
        NarrativeScene scene = base.requireScene(operation.sceneId());
        return NarrativeEditorReplaceScene.replaceScene(base.novel(), scene.withInitialMeta(operation.initialMeta()));
    }
}

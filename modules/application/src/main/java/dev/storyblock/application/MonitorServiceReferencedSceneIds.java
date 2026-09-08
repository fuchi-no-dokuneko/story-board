package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashSet;
import java.util.Set;

final class MonitorServiceReferencedSceneIds {
    static Set<Ids.SceneId> referencedSceneIds(EditOperation operation) {
        Set<Ids.SceneId> result = new LinkedHashSet<>();
        if (operation instanceof EditOperation.InsertBlocks value) {
            result.add(value.insertionPoint().sceneId());
        } else if (operation instanceof EditOperation.ReplaceBlockRange value) {
            result.add(value.range().sceneId());
        } else if (operation instanceof EditOperation.DeleteBlockRange value) {
            result.add(value.range().sceneId());
        } else if (operation instanceof EditOperation.SplitBlock value) {
            result.add(value.block().sceneId());
        } else if (operation instanceof EditOperation.MergeBlocks value) {
            result.add(value.range().sceneId());
        } else if (operation instanceof EditOperation.ExtendBlock value) {
            result.add(value.block().sceneId());
        } else if (operation instanceof EditOperation.MoveBlockRange value) {
            result.add(value.range().sceneId());
            result.add(value.destination().sceneId());
            result.add(value.expectedSourceBoundary().sceneId());
            result.add(value.expectedDestinationBoundary().sceneId());
        } else if (operation instanceof EditOperation.CorrectBlockMeta value) {
            result.add(value.sceneId());
        } else if (operation instanceof EditOperation.SetSceneInitialMeta value) {
            result.add(value.sceneId());
            result.add(value.expectedBoundary().sceneId());
        }
        return Set.copyOf(result);
    }
}

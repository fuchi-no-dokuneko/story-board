package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashSet;
import java.util.Set;

final class MonitorServiceReferencedBlockIds {
    static Set<Ids.BlockId> referencedBlockIds(EditOperation operation) {
        Set<Ids.BlockId> result = new LinkedHashSet<>();
        if (operation instanceof EditOperation.InsertBlocks value) {
            MonitorServiceAdd.add(result, value.insertionPoint().anchorBlockId());
        } else if (operation instanceof EditOperation.ReplaceBlockRange value) {
            MonitorServiceAddRange.addRange(result, value.range());
        } else if (operation instanceof EditOperation.DeleteBlockRange value) {
            MonitorServiceAddRange.addRange(result, value.range());
        } else if (operation instanceof EditOperation.SplitBlock value) {
            MonitorServiceAddRange.addRange(result, value.block());
        } else if (operation instanceof EditOperation.MergeBlocks value) {
            MonitorServiceAddRange.addRange(result, value.range());
        } else if (operation instanceof EditOperation.ExtendBlock value) {
            MonitorServiceAddRange.addRange(result, value.block());
        } else if (operation instanceof EditOperation.MoveBlockRange value) {
            MonitorServiceAddRange.addRange(result, value.range());
            MonitorServiceAdd.add(result, value.destination().anchorBlockId());
            MonitorServiceAddBoundary.addBoundary(result, value.expectedSourceBoundary());
            MonitorServiceAddBoundary.addBoundary(result, value.expectedDestinationBoundary());
        } else if (operation instanceof EditOperation.CorrectBlockMeta value) {
            result.add(value.block().blockId());
        } else if (operation instanceof EditOperation.SetSceneInitialMeta value) {
            MonitorServiceAddBoundary.addBoundary(result, value.expectedBoundary());
        }
        return Set.copyOf(result);
    }
}

package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorBlockFingerprint;
import dev.storyblock.monitor.MonitorPacket;
import java.util.Set;

final class MonitorServiceValidateProposal {
    static void validateProposal(
            MonitorPacket packet,
            Set<Ids.BlockId> affected,
            EditOperation operation
    ) {
        if (!operation.context().novelId().equals(packet.novelId())
                || !operation.context().baseRevisionId().equals(packet.revisionId())
                || !operation.context().expectedHeadHash().equals(packet.revisionHash())) {
            throw new IllegalArgumentException(
                    "Monitor proposal context must match the source packet"
            );
        }
        if (operation instanceof EditOperation.RestoreRevisionContent) {
            throw new IllegalArgumentException(
                    "Monitor proposals cannot restore global revision content"
            );
        }
        Set<Ids.BlockId> window = packet.localInvariants().windowBlocks().stream()
                .map(MonitorBlockFingerprint::blockId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Ids.BlockId> references = MonitorServiceReferencedBlockIds.referencedBlockIds(operation);
        if (!window.containsAll(references)) {
            throw new IllegalArgumentException(
                    "Monitor proposal block references must remain inside the packet window"
            );
        }
        Set<Ids.SceneId> windowScenes = packet.renderPacket().sceneBoundaries().stream()
                .map(boundary -> boundary.sceneId())
                .collect(java.util.stream.Collectors.toSet());
        if (!windowScenes.containsAll(MonitorServiceReferencedSceneIds.referencedSceneIds(operation))) {
            throw new IllegalArgumentException(
                    "Monitor proposal scene references must remain inside the packet window"
            );
        }
        Set<Ids.BlockId> directlyChanged = MonitorServiceDirectlyChangedBlockIds.directlyChangedBlockIds(operation);
        if (!affected.containsAll(directlyChanged)) {
            throw new IllegalArgumentException(
                    "Monitor proposal affected IDs must include every changed source block"
            );
        }
    }
}

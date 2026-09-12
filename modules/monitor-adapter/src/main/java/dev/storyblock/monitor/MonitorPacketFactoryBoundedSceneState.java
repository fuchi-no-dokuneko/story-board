package dev.storyblock.monitor;

import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static dev.storyblock.monitor.MonitorPacketFactory.BlockLocation;

final class MonitorPacketFactoryBoundedSceneState {
    static RenderPacket boundedSceneState(
            RenderPacket render,
            List<BlockLocation> blocks,
            int from,
            int to
    ) {
        List<DerivedSceneBoundary> boundaries = new ArrayList<>();
        int globalIndex = from;
        int localIndex = 0;
        while (globalIndex <= to) {
            Ids.SceneId sceneId = blocks.get(globalIndex).sceneId();
            Map<String, Object> stateIn = render.resolvedMetadata().get(localIndex).before();
            Map<String, Object> stateOut = render.resolvedMetadata().get(localIndex).after();
            while (globalIndex + 1 <= to
                    && blocks.get(globalIndex + 1).sceneId().equals(sceneId)) {
                globalIndex++;
                localIndex++;
                stateOut = render.resolvedMetadata().get(localIndex).after();
            }
            boundaries.add(new DerivedSceneBoundary(sceneId, stateIn, stateOut));
            globalIndex++;
            localIndex++;
        }
        return new RenderPacket(
                render.novelId(),
                render.revisionId(),
                render.revisionHash(),
                render.rendererVersion(),
                render.range(),
                render.renderedText(),
                render.blocks(),
                render.resolvedMetadata(),
                render.offsetMap(),
                boundaries
        );
    }
}

package dev.storyblock.detector;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderedBlock;
import dev.storyblock.renderer.ResolvedBlockMetadata;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorBlockContexts {
    static List<BlockContext> blockContexts(
            List<NarrativeScene> scenes,
            RenderPacket packet
    ) {
        List<BlockContext> result = new ArrayList<>();
        int globalIndex = 0;
        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            NarrativeScene scene = scenes.get(sceneIndex);
            for (int blockIndex = 0; blockIndex < scene.blocks().size(); blockIndex++) {
                NarrativeBlock block = scene.blocks().get(blockIndex);
                RenderedBlock rendered = packet.blocks().get(globalIndex);
                ResolvedBlockMetadata resolved = packet.resolvedMetadata().get(globalIndex);
                if (!block.id().equals(rendered.blockId())
                        || !block.id().equals(resolved.blockId())) {
                    throw new IllegalArgumentException("Render packet block order does not match revision");
                }
                result.add(new BlockContext(
                        scene,
                        sceneIndex,
                        blockIndex == 0,
                        rendered,
                        resolved
                ));
                globalIndex++;
            }
        }
        if (globalIndex != packet.blocks().size()) {
            throw new IllegalArgumentException("Render packet contains blocks outside the revision");
        }
        return List.copyOf(result);
    }
}

package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.renderer.ResolvedBlockMetadata;
import java.util.Set;

final class DeterministicValidatorPresenceBeforeAction {
    static Set<String> presenceBefore(DeterministicValidator self, RevisionManifest base, String baseHash, NarrativeScene scene, int index)  {
        if (index == 0) {
            return scene.initialMeta() == null
                    ? Set.of()
                    : DeterministicValidatorStrings.strings(scene.initialMeta().fields().get("present_character_ids"));
        }
        Ids.BlockId previous = scene.blocks().get(index - 1).id();
        RenderPacket packet = self.renderer.render(base, baseHash, RenderRange.all());
        for (ResolvedBlockMetadata metadata : packet.resolvedMetadata()) {
            if (metadata.blockId().equals(previous)) {
                return DeterministicValidatorStrings.strings(metadata.after().get("present_character_ids"));
            }
        }
        throw new IllegalArgumentException("Renderer omitted preceding block " + previous.value());
    }
}

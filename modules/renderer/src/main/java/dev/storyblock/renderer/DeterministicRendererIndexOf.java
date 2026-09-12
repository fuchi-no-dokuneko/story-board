package dev.storyblock.renderer;

import dev.storyblock.domain.Ids;
import java.util.List;
import static dev.storyblock.renderer.DeterministicRenderer.ResolvedEntry;

final class DeterministicRendererIndexOf {
    static int indexOf(List<ResolvedEntry> entries, Ids.BlockId blockId) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).block().id().equals(blockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Revision does not contain render endpoint " + blockId.value()
        );
    }
}

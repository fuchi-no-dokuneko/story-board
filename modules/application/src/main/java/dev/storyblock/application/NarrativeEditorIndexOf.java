package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import java.util.List;

final class NarrativeEditorIndexOf {
    static int indexOf(List<NarrativeBlock> blocks, Ids.BlockId id) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).id().equals(id)) {
                return index;
            }
        }
        throw NarrativeEditorInvalid.invalid("Block is not present in the expected scene: " + id.value());
    }
}

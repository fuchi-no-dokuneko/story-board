package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import java.util.List;

final class DeterministicValidatorIndexOf {
    static int indexOf(List<NarrativeBlock> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).id().equals(blockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Scene does not contain block " + blockId.value());
    }
}

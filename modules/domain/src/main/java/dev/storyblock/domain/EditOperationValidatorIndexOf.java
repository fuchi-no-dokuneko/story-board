package dev.storyblock.domain;

import java.util.List;

final class EditOperationValidatorIndexOf {
    static int indexOf(List<NarrativeBlock> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).id().equals(blockId)) {
                return index;
            }
        }
        throw EditOperationValidatorAdjacency.adjacency("Block is not present in the expected scene: " + blockId.value());
    }
}

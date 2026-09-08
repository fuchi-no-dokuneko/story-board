package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class EditOperationRequireDistinctDrafts {
    static List<BlockDraft> requireDistinctDrafts(
            List<BlockDraft> drafts,
            int minimum,
            String operation
    ) {
        drafts = List.copyOf(drafts);
        if (drafts.size() < minimum) {
            throw new IllegalArgumentException(operation + " requires at least " + minimum + " block(s)");
        }
        Set<Ids.BlockId> ids = new HashSet<>();
        for (BlockDraft draft : drafts) {
            Objects.requireNonNull(draft, "block draft");
            if (!ids.add(draft.id())) {
                throw new IllegalArgumentException(operation + " cannot repeat a draft block ID");
            }
        }
        return drafts;
    }
}

package dev.storyblock.rewrite;

import dev.storyblock.domain.UnicodeText;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import static dev.storyblock.rewrite.RewriteWorkerInput.*;

final class RewriteWorkerInputValidation {
  static void validate(List<RewriteSourceBlock> blocks, RewriteConstraints constraints) {
    if (blocks.isEmpty() || blocks.size() > RewriteModule.MAX_SOURCE_BLOCKS
            || new HashSet<>(blocks.stream().map(
                RewriteSourceBlock::blockId
            ).toList()).size() != blocks.size()
            || new HashSet<>(blocks.stream().map(
                RewriteSourceBlock::blockVersionId
            ).toList()).size() != blocks.size()) {
          throw new IllegalArgumentException("Rewrite source block range is invalid");
        }
        int firstEditable = -1;
        int lastEditable = -1;
        int editableCount = 0;
        int totalGraphemes = 0;
        for (int index = 0; index < blocks.size(); index++) {
          RewriteSourceBlock block = blocks.get(index);
          totalGraphemes += UnicodeText.graphemeCount(block.text());
          if (block.editable()) {
            if (firstEditable < 0) {
              firstEditable = index;
            }
            lastEditable = index;
            editableCount++;
          }
        }
        if (editableCount < 1 || editableCount > RewriteModule.MAX_EDITABLE_BLOCKS
            || firstEditable > RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE
            || blocks.size() - lastEditable - 1
            > RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE
            || totalGraphemes > RewriteModule.MAX_SOURCE_BLOCKS
            * UnicodeText.MAX_BLOCK_GRAPHEMES) {
          throw new IllegalArgumentException("Rewrite editable range is not minimal");
        }
        for (int index = firstEditable; index <= lastEditable; index++) {
          if (!blocks.get(index).editable()) {
            throw new IllegalArgumentException(
                "Rewrite editable blocks must form one contiguous range"
            );
          }
        }
        Objects.requireNonNull(constraints, "constraints");
        if (constraints.maxChangedBlocks() > editableCount
            || constraints.maxOutputGraphemes()
            > editableCount * UnicodeText.MAX_BLOCK_GRAPHEMES) {
          throw new IllegalArgumentException(
              "Rewrite constraints exceed the editable source range"
          );
        }
  }
}

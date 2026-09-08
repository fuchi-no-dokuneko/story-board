package dev.storyblock.domain;

import java.util.Objects;
import static dev.storyblock.domain.EditOperation.*;

public interface BlockRangeOperations {
  record DeleteBlockRange(EditContext context, BlockRangeGuard range) implements EditOperation {
    public DeleteBlockRange {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(range, "range");
    }

    @Override
    public Type type() {
      return Type.DELETE_BLOCK_RANGE;
    }
  }

  record ExtendBlock(
      EditContext context,
      BlockRangeGuard block,
      ExtensionPosition position,
      BlockDraft replacement
  ) implements EditOperation {
    public ExtendBlock {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(block, "block");
      Objects.requireNonNull(position, "position");
      Objects.requireNonNull(replacement, "replacement");
      if (block.expectedBlocks().size() != 1) {
        throw new IllegalArgumentException("extend_block must guard exactly one block");
      }
    }

    @Override
    public Type type() {
      return Type.EXTEND_BLOCK;
    }
  }
}

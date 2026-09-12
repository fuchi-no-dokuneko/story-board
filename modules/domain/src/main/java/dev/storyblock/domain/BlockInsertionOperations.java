package dev.storyblock.domain;

import java.util.List;
import java.util.Objects;
import static dev.storyblock.domain.EditOperation.*;

public interface BlockInsertionOperations {
  record InsertBlocks(
      EditContext context,
      InsertionPoint insertionPoint,
      List<BlockDraft> blocks
  ) implements EditOperation {
    public InsertBlocks {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(insertionPoint, "insertionPoint");
      blocks = EditOperationRequireDistinctDrafts.requireDistinctDrafts(blocks, 1, "insert_blocks");
    }

    @Override
    public Type type() {
      return Type.INSERT_BLOCKS;
    }
  }

  record ReplaceBlockRange(
      EditContext context,
      BlockRangeGuard range,
      List<BlockDraft> newBlocks
  ) implements EditOperation {
    public ReplaceBlockRange {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(range, "range");
      newBlocks = EditOperationRequireDistinctDrafts.requireDistinctDrafts(newBlocks, 1, "replace_block_range");
    }

    @Override
    public Type type() {
      return Type.REPLACE_BLOCK_RANGE;
    }
  }
}

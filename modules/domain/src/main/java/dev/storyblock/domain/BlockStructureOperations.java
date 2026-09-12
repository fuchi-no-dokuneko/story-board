package dev.storyblock.domain;

import java.util.List;
import java.util.Objects;
import static dev.storyblock.domain.EditOperation.*;

public interface BlockStructureOperations {
  record SplitBlock(
      EditContext context,
      BlockRangeGuard block,
      int splitAfterGrapheme,
      List<BlockDraft> newBlocks
  ) implements EditOperation {
    public SplitBlock {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(block, "block");
      if (block.expectedBlocks().size() != 1) {
        throw new IllegalArgumentException("split_block must guard exactly one block");
      }
      if (splitAfterGrapheme < 1) {
        throw new IllegalArgumentException("Split anchor must be a positive grapheme offset");
      }
      newBlocks = EditOperationRequireDistinctDrafts.requireDistinctDrafts(newBlocks, 2, "split_block");
      if (newBlocks.size() != 2) {
        throw new IllegalArgumentException("split_block must produce exactly two blocks");
      }
    }

    @Override
    public Type type() {
      return Type.SPLIT_BLOCK;
    }

    public BlockProvenanceMapping provenanceMapping() {
      return BlockProvenanceMapping.split(block.expectedBlocks().getFirst(), newBlocks);
    }
  }

  record MergeBlocks(
      EditContext context,
      BlockRangeGuard range,
      BlockDraft newBlock
  ) implements EditOperation {
    public MergeBlocks {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(range, "range");
      Objects.requireNonNull(newBlock, "newBlock");
      if (range.expectedBlocks().size() < 2) {
        throw new IllegalArgumentException("merge_blocks requires at least two blocks");
      }
    }

    @Override
    public Type type() {
      return Type.MERGE_BLOCKS;
    }

    public BlockProvenanceMapping provenanceMapping() {
      return BlockProvenanceMapping.merge(range.expectedBlocks(), newBlock);
    }
  }
}
